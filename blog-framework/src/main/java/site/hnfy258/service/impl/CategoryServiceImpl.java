package site.hnfy258.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
}

