package site.hnfy258.service;

import com.baomidou.mybatisplus.extension.service.IService;
import site.hnfy258.DTO.ArticleDto;
import site.hnfy258.VO.ArticleDetailVo;
import site.hnfy258.VO.ArticleVo;
import site.hnfy258.VO.HotArticleVo;
import site.hnfy258.VO.PageVo;
import site.hnfy258.domain.ResponseResult;
import site.hnfy258.entity.Article;

import java.util.List;

public interface ArticleService extends IService<Article> {
    List<HotArticleVo> hotArticleList();

    PageVo articleList(Integer pageNum, Integer pageSize, Long categoryId);

    ArticleDetailVo getArticleDetail(Long id);

    ResponseResult updateViewCount(Long id);

    void add(ArticleDto articleDTO);

    PageVo selectArticlePage(Article article, Integer pageNum, Integer pageSize);

    void edit(ArticleDto article);

    ArticleVo getInfo(Long id);

}
