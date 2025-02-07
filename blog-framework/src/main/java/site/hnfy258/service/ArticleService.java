package site.hnfy258.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.pagehelper.Page;
import site.hnfy258.domain.ResponseResult;
import site.hnfy258.domain.VO.ArticleDetailVo;
import site.hnfy258.domain.VO.ArticleListVo;
import site.hnfy258.domain.VO.HotArticleVo;
import site.hnfy258.domain.VO.PageVo;
import site.hnfy258.domain.entity.Article;

import java.util.List;

public interface ArticleService extends IService<Article> {
    List<HotArticleVo> hotArticleList();

    PageVo articleList(Integer pageNum, Integer pageSize, Long categoryId);

    ArticleDetailVo getArticleDetail(Long id);
}
