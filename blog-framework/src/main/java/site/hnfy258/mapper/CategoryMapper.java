package site.hnfy258.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import site.hnfy258.entity.Category;

import java.util.List;


/**
 * 分类表(Category)表数据库访问层
 *
 * @author makejava
 * @since 2025-02-07 06:54:19
 *
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}

