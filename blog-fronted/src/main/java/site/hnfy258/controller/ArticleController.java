package site.hnfy258.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.hnfy258.domain.ResponseResult;
import site.hnfy258.domain.VO.HotArticleVo;
import site.hnfy258.domain.entity.Article;
import site.hnfy258.service.ArticleService;

import java.util.List;

@RestController
@RequestMapping("/article")
public class ArticleController {
    @Autowired
    private ArticleService articleService;
    @GetMapping("/hotArticleList")
    public ResponseResult hotArticleList(){
        List<HotArticleVo> articles =articleService.hotArticleList();
        return ResponseResult.okResult(articles);
    }
}
