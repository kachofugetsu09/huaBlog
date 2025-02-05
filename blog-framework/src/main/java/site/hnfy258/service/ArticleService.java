package site.hnfy258.service;

import com.baomidou.mybatisplus.extension.service.IService;
import site.hnfy258.domain.ResponseResult;
import site.hnfy258.domain.VO.HotArticleVo;
import site.hnfy258.domain.entity.Article;

import java.util.List;

public interface ArticleService extends IService<Article> {
    List<HotArticleVo> hotArticleList();
}
