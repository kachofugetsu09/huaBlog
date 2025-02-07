package site.hnfy258.controller;

import com.github.pagehelper.Page;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.hnfy258.domain.ResponseResult;
import site.hnfy258.domain.VO.ArticleListVo;
import site.hnfy258.domain.VO.ArticleListVo;
import site.hnfy258.domain.VO.HotArticleVo;
import site.hnfy258.domain.VO.PageVo;
import site.hnfy258.domain.entity.Article;
import site.hnfy258.service.ArticleService;

import java.util.List;

@RestController
@RequestMapping("/article")
public class ArticleController {
    @Autowired
    private ArticleService articleService;
    @GetMapping("/hotArticleList")
    @ApiOperation("获取热门文章")
    public ResponseResult hotArticleList(){
        List<HotArticleVo> articles =articleService.hotArticleList();
        return ResponseResult.okResult(articles);
    }
    @GetMapping("/articleList")
    @ApiOperation("获取文章列表")
    public ResponseResult<PageVo> articleList(Integer pageNum, Integer pageSize, Long categoryId){
        PageVo pageVo = articleService.articleList(pageNum,pageSize,categoryId);
            return ResponseResult.okResult(pageVo);
    }
}

