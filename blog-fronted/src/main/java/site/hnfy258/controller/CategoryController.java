package site.hnfy258.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.hnfy258.domain.ResponseResult;
import site.hnfy258.domain.VO.CategoryVo;
import site.hnfy258.service.CategoryService;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/category")
@Api(tags = "分类管理")

public class CategoryController {
    @Autowired
    private CategoryService categoryService;
    @GetMapping("/getCategoryList")
    public ResponseResult<List<CategoryVo>> getCatoryList(){
        List<CategoryVo> categoryVoList = categoryService.getCategoryList();
        return ResponseResult.okResult(categoryVoList);
    }

}
