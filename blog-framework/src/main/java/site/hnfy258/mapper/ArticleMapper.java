package site.hnfy258.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.mybatis.spring.annotation.MapperScan;
import site.hnfy258.domain.entity.Article;

import java.util.List;

@Mapper

public interface ArticleMapper extends BaseMapper<Article> {
    @Select("SELECT id, title, view_count FROM sg_article WHERE status=0 ORDER BY view_count DESC LIMIT 10")
    List<Article> selectHotArticleList();
}
