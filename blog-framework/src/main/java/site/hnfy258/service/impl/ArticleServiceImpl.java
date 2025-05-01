package site.hnfy258.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import site.hnfy258.DTO.ArticleDto;
import site.hnfy258.VO.*;
import site.hnfy258.config.RabbitMQConfig;
import site.hnfy258.constants.SystemConstants;
import site.hnfy258.domain.ResponseResult;
import site.hnfy258.entity.Article;
import site.hnfy258.entity.ArticleTag;
import site.hnfy258.entity.Category;
import site.hnfy258.entity.User;
import site.hnfy258.mapper.ArticleMapper;
import site.hnfy258.service.ArticleModerationService;
import site.hnfy258.service.ArticleService;
import site.hnfy258.service.CategoryService;
import site.hnfy258.utils.BeanCopyUtils;
import com.github.pagehelper.PageHelper;
import site.hnfy258.utils.RedisCache;
import site.hnfy258.utils.SecurityUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {
    /**
     * @return
     */
    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private RedisCache redisCache;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private ArticleTagServiceImpl articleTagService;

    @Autowired
    private ArticleModerationService moderationService;

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Override
    public List<HotArticleVo> hotArticleList() {
        List<Article> articleList = articleMapper.selectHotArticleList();
//        for(Article a:articleList){
//            HotArticleVo hotArticleVo = new HotArticleVo();
//            BeanUtils.copyProperties(a,hotArticleVo);
//            hotArticleVoList.add(hotArticleVo);
//        }
        List<HotArticleVo> hotArticleVoList = BeanCopyUtils.copyBeanList(articleList, HotArticleVo.class);
        return hotArticleVoList;
    }

    /**
     * @param pageNum
     * @param pageSize
     * @param categoryId
     * @return
     */
    @Override
    public PageVo articleList(Integer pageNum, Integer pageSize, Long categoryId) {
        // 开启分页
        PageHelper.startPage(pageNum, pageSize);

        // 调用 Mapper 方法查询文章列表
        List<Article> articles = articleMapper.getArticleList(categoryId, SystemConstants.ARTICLE_STATUS_NORMAL);

        // 将查询结果转换为 VO 对象
        List<ArticleListVo> articleListVos = articles.stream().map(article -> {
            ArticleListVo vo = BeanCopyUtils.copyBean(article, ArticleListVo.class);
            String content = article.getContent();
            int wordCount = 0;
            if (StringUtils.hasText(content)) {
                wordCount = content.replaceAll("</?[^>]+>", "").length();
            }

            // 设置字数
            vo.setWordCount(wordCount);

            return vo;
        }).collect(Collectors.toList());

        // 获取分页信息
        PageInfo<Article> pageInfo = new PageInfo<>(articles);

        // 封装结果
        PageVo pageVo = new PageVo(articleListVos, pageInfo.getTotal());
        return pageVo;
    }

    /**
     * @param id
     * @return
     */
    @Override
    public ArticleDetailVo getArticleDetail(Long id) {
        //根据id查询文章
        Article article = getById(id);
        //从redis中获取viewCount
        Integer viewCount = redisCache.getCacheMapValue("article:viewCount", id.toString());
        article.setViewCount(viewCount.longValue());
        //转换成VO
        ArticleDetailVo articleDetailVo = BeanCopyUtils.copyBean(article, ArticleDetailVo.class);
        //根据分类id查询分类
        Long categoryId = articleDetailVo.getCategoryId();
        Category category = categoryService.getById(categoryId);
        if(category!=null){
            articleDetailVo.setCategoryName(category.getName());
        }
        //封装响应返回
        return articleDetailVo;
    }

    @Override
    public ResponseResult updateViewCount(Long id) {
        //更新redis中对应 id的浏览量
        redisCache.incrementCacheMapValue("article:viewCount",id.toString(),1);
        return ResponseResult.okResult();
    }

    /**
     * @param articleDTO
     */
    @Override
    @Transactional
    public void add(ArticleDto articleDTO) {
        log.info("添加文章：{}", articleDTO);
        Article article = new Article();
        BeanUtils.copyProperties(articleDTO,article);
        Long userId = SecurityUtils.getUserId();
        article.setCreateBy(userId);
        // 设置状态为草稿(1)，等待审核通过后改为已发布(0)
        article.setStatus("1");
        save(article);
        System.out.println(article.getId());

        List<ArticleTag> articleTags = articleDTO.getTags().stream().map(
                tagId ->new ArticleTag(article.getId(),tagId)
        ).collect(Collectors.toList());
        articleTagService.saveBatch(articleTags);

        // 将文章发送到RabbitMQ进行内容审核
        try {
            String jsonArticle = JSON.toJSONString(article);
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.ARTICLE_EXCHANGE,
                RabbitMQConfig.ARTICLE_MODERATION_ROUTING_KEY,
                jsonArticle
            );

            log.info("发送文章审核消息成功，文章ID: {}", article.getId());
        } catch (Exception e) {
            log.error("发送文章审核消息失败", e);

            if (moderationService.checkArticleContent(article)) {
                moderationService.approveArticle(article.getId());
            }
        }
    }


    /**
     * @param article
     * @param pageNum
     * @param pageSize
     * @return
     */
    @Override
    public PageVo selectArticlePage(Article article, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper();

        queryWrapper.like(StringUtils.hasText(article.getTitle()),Article::getTitle, article.getTitle());
        queryWrapper.like(StringUtils.hasText(article.getSummary()),Article::getSummary, article.getSummary());

        Page<Article> page = new Page<>();
        page.setCurrent(pageNum);
        page.setSize(pageSize);
        page(page,queryWrapper);

        List<Article> articles = page.getRecords();
        PageVo pageVo = new PageVo();
        pageVo.setTotal(page.getTotal());
        pageVo.setRows(articles);
        return pageVo;
    }

    @Override
    public ArticleVo getInfo(Long id) {
        Article article = getById(id);
        //获取关联标签
        LambdaQueryWrapper<ArticleTag> articleTagLambdaQueryWrapper = new LambdaQueryWrapper<>();
        articleTagLambdaQueryWrapper.eq(ArticleTag::getArticleId,article.getId());
        List<ArticleTag> articleTags = articleTagService.list(articleTagLambdaQueryWrapper);
        List<Long> tags = articleTags.stream().map(articleTag -> articleTag.getTagId()).collect(Collectors.toList());

        ArticleVo articleVo = BeanCopyUtils.copyBean(article,ArticleVo.class);
        articleVo.setTags(tags);
        return articleVo;
    }

    @Override
    public void edit(ArticleDto articleDto) {
        Article article = BeanCopyUtils.copyBean(articleDto, Article.class);
        //更新博客信息
        updateById(article);
        //删除原有的 标签和博客的关联
        LambdaQueryWrapper<ArticleTag> articleTagLambdaQueryWrapper = new LambdaQueryWrapper<>();
        articleTagLambdaQueryWrapper.eq(ArticleTag::getArticleId,article.getId());
        articleTagService.remove(articleTagLambdaQueryWrapper);
        //添加新的博客和标签的关联信息
        List<ArticleTag> articleTags = articleDto.getTags().stream()
                .map(tagId -> new ArticleTag(articleDto.getId(), tagId))
                .collect(Collectors.toList());
        articleTagService.saveBatch(articleTags);

    }
}
