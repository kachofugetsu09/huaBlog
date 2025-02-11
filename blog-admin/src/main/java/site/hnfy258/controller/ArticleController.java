package site.hnfy258.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.hnfy258.DTO.AddArticleDto;
import site.hnfy258.domain.ResponseResult;
import site.hnfy258.service.ArticleService;

@RestController
@RequestMapping("/content/article")

public class ArticleController {
    @Autowired
    ArticleService articleService;
    @PostMapping
    public ResponseResult addArticle(@RequestBody AddArticleDto articleDTO ){
        articleService.add(articleDTO);
        return ResponseResult.okResult();
    }
}
