package site.hnfy258.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import site.hnfy258.entity.Article;

import java.util.List;

@Mapper

public interface ArticleMapper extends BaseMapper<Article> {
    @Select("SELECT id, title, view_count FROM sg_article WHERE status=0 ORDER BY view_count DESC LIMIT 10")
    List<Article> selectHotArticleList();

    List<Long> getAllCategoryIds(@Param("status") int status);
    @Select("SELECT * FROM sg_article WHERE id = #{id}")
    Article getById(Long id);
    List<Article> getArticleList(@Param("categoryId") Long categoryId, @Param("status") Integer status);
    List<Article> getArticlesWithCategoryName(@Param("categoryId") Long categoryId,
                                              @Param("status") int status);


}
