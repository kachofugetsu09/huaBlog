package site.hnfy258.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.mybatis.spring.annotation.MapperScan;
import site.hnfy258.domain.entity.Article;

import java.util.List;
import java.util.Set;

@Mapper

public interface ArticleMapper extends BaseMapper<Article> {
    @Select("SELECT id, title, view_count FROM sg_article WHERE status=0 ORDER BY view_count DESC LIMIT 10")
    List<Article> selectHotArticleList();

    List<Long> getAllCategoryIds(@Param("status") int status);
    @Select("SELECT * FROM sg_article WHERE category_id = #{categoryId} AND status = #{status}")
    List<Article> getAllArticleByCategoryId(@Param("categoryId") Long categoryId, @Param("status") int status);;
}
