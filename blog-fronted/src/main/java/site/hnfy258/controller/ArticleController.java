package site.hnfy258.controller;

import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.hnfy258.domain.ResponseResult;
import site.hnfy258.VO.HotArticleVo;
import site.hnfy258.VO.PageVo;
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
    @GetMapping("/{id}")
    @ApiOperation("获取文章详情")
    public ResponseResult getArticleDetail(@PathVariable("id") Long id){
        return ResponseResult.okResult(articleService.getArticleDetail(id));
    }

}

