package site.hnfy258.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.hnfy258.domain.ResponseResult;
import site.hnfy258.service.ArticleService;

@RestController
@RequestMapping("/article")
public class ArticleController {
    @Autowired
    private ArticleService articleService;
    @GetMapping("/hotArticleList")
    public ResponseResult hotArticleList(){
        ResponseResult result =articleService.hotArticleList();
        return result;
    }
}
