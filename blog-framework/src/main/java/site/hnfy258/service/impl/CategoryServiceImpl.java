package site.hnfy258.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import site.hnfy258.VO.PageVo;
import site.hnfy258.constants.SystemConstants;
import site.hnfy258.VO.CategoryVo;
import site.hnfy258.entity.Category;
import site.hnfy258.mapper.ArticleMapper;
import site.hnfy258.mapper.CategoryMapper;
import site.hnfy258.service.CategoryService;
import site.hnfy258.utils.BeanCopyUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 分类表(Category)表服务实现类
 *
 * @author makejava
 * @since 2025-02-07 06:52:32
 */
@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {
    @Autowired
    CategoryMapper categoryMapper;
    @Autowired
    ArticleMapper articleMapper;

    /**
     * @return
     */
    @Override
    public List<CategoryVo> getCategoryList() {
        List<Long> categoryIdList =articleMapper.getAllCategoryIds(SystemConstants.ARTICLE_STATUS_NORMAL);
        Set<Long> categoryIds = categoryIdList.stream().collect(Collectors.toSet());

        if(categoryIds.size()>0){
            List<Category> categoryList = categoryMapper.selectBatchIds(categoryIds);
            List<CategoryVo> categoryVos = BeanCopyUtils.copyBeanList(categoryList, CategoryVo.class);
            return categoryVos;
        }
        else {
            return null;
        }
    }

    @Override
    public List<CategoryVo> listAllCategory() {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getStatus, SystemConstants.NORMAL);
        List<Category> list = list(wrapper);
        List<CategoryVo> categoryVos = BeanCopyUtils.copyBeanList(list, CategoryVo.class);
        return categoryVos;
    }

    @Override
    public PageVo selectCategoryPage(Category category, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper();

        queryWrapper.like(StringUtils.hasText(category.getName()),Category::getName, category.getName());
        queryWrapper.eq(Objects.nonNull(category.getStatus()),Category::getStatus, category.getStatus());

        Page<Category> page = new Page<>();
        page.setCurrent(pageNum);
        page.setSize(pageSize);
        page(page,queryWrapper);

        //转换成VO
        List<Category> categories = page.getRecords();

        PageVo pageVo = new PageVo();
        pageVo.setTotal(page.getTotal());
        pageVo.setRows(categories);
        return pageVo;
    }
}

