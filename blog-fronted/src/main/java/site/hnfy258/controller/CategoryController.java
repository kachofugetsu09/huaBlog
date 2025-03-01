package site.hnfy258.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.hnfy258.annotation.SystemLog;
import site.hnfy258.domain.ResponseResult;
import site.hnfy258.VO.CategoryVo;
import site.hnfy258.service.CategoryService;

import java.util.List;

@RestController
@RequestMapping("/category")
@Api(tags = "分类管理")

public class CategoryController {
    @Autowired
    private CategoryService categoryService;
    @SystemLog(bussinessName = "获取分类列表信息")
    @GetMapping("/getCategoryList")
    @ApiOperation("获取分类列表")
    public ResponseResult<List<CategoryVo>> getCatoryList(){
        List<CategoryVo> categoryVoList = categoryService.getCategoryList();
        return ResponseResult.okResult(categoryVoList);
    }

}
