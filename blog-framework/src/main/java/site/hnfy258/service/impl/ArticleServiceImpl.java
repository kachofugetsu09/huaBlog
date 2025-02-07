package site.hnfy258.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import site.hnfy258.constants.SystemConstants;
import site.hnfy258.domain.VO.ArticleListVo;
import site.hnfy258.domain.VO.HotArticleVo;
import site.hnfy258.domain.VO.PageVo;
import site.hnfy258.domain.entity.Article;
import site.hnfy258.mapper.ArticleMapper;
import site.hnfy258.service.ArticleService;
import site.hnfy258.utils.BeanCopyUtils;
import com.github.pagehelper.PageHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        // 开始分页
        Page<Article> page = PageHelper.startPage(pageNum, pageSize);

        // 查询文章列表
        List<Article> articles = articleMapper.getAllArticleByCategoryId(categoryId, SystemConstants.ARTICLE_STATUS_NORMAL);

        // 将文章列表转换为目标 VO 列表
        List<ArticleListVo> articleListVos = BeanCopyUtils.copyBeanList(articles, ArticleListVo.class);
        PageVo pagevo = new PageVo(articleListVos, page.getTotal());


        return pagevo;
    }
}
