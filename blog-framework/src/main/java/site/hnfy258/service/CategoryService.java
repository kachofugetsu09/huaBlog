package site.hnfy258.service;

import com.baomidou.mybatisplus.extension.service.IService;
import site.hnfy258.VO.CategoryVo;
import site.hnfy258.VO.PageVo;
import site.hnfy258.entity.Category;

import java.util.List;


/**
 * 分类表(Category)表服务接口
 *
 * @author makejava
 * @since 2025-02-07 06:50:37
 */
public interface CategoryService extends IService<Category> {

    List<CategoryVo> getCategoryList();

    PageVo selectCategoryPage(Category category, Integer pageNum, Integer pageSize);

    List<CategoryVo> listAllCategory();
}

