package site.hnfy258.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import site.hnfy258.constants.SystemConstants;
import site.hnfy258.VO.ArticleDetailVo;
import site.hnfy258.VO.ArticleListVo;
import site.hnfy258.VO.HotArticleVo;
import site.hnfy258.VO.PageVo;
import site.hnfy258.entity.Article;
import site.hnfy258.mapper.ArticleMapper;
import site.hnfy258.service.ArticleService;
import site.hnfy258.utils.BeanCopyUtils;
import com.github.pagehelper.PageHelper;

import java.util.List;
import java.util.stream.Collectors;

@Service

public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {
    /**
     * @return
     */
    @Autowired
    private ArticleMapper articleMapper;
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
        List<ArticleListVo> articleListVos = BeanCopyUtils.copyBeanList(articles, ArticleListVo.class);

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
        Article article = articleMapper.getById(id);
        ArticleDetailVo articleDetailVo = BeanCopyUtils.copyBean(article, ArticleDetailVo.class);
        return articleDetailVo;
    }
}
