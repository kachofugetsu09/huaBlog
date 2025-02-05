package site.hnfy258.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import site.hnfy258.domain.entity.Article;
@Mapper

public interface ArticleMapper extends BaseMapper<Article> {
}
