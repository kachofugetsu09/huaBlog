package site.hnfy258.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import site.hnfy258.DTO.ArticleDto;
import site.hnfy258.VO.ArticleVo;
import site.hnfy258.VO.PageVo;
import site.hnfy258.domain.ResponseResult;
import site.hnfy258.entity.Article;
import site.hnfy258.service.ArticleService;

@RestController
@RequestMapping("/content/article")

public class ArticleController {
    @Autowired
    ArticleService articleService;
    @PostMapping
    public ResponseResult addArticle(@RequestBody ArticleDto articleDTO ){
        articleService.add(articleDTO);
        return ResponseResult.okResult();
    }
    @GetMapping("/list")
    public ResponseResult list(Article article, Integer pageNum, Integer pageSize)
    {
        PageVo pageVo = articleService.selectArticlePage(article,pageNum,pageSize);
        return ResponseResult.okResult(pageVo);
    }

    @GetMapping(value = "/{id}")
    public ResponseResult getInfo(@PathVariable(value = "id")Long id){
        ArticleVo article = articleService.getInfo(id);
        return ResponseResult.okResult(article);
    }

    @PutMapping
    public ResponseResult edit(@RequestBody ArticleDto article){
        articleService.edit(article);
        return ResponseResult.okResult();
    }
    @DeleteMapping("/{id}")
    public ResponseResult delete(@PathVariable Long id){
        articleService.removeById(id);
        return ResponseResult.okResult();
    }

    }
