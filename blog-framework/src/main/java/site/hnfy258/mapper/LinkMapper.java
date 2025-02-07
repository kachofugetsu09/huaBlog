package site.hnfy258.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import org.mapstruct.Mapper;
import site.hnfy258.entity.Link;

import java.util.List;


/**
 * 友链(SgLink)表数据库访问层
 *
 * @author makejava
 * @since 2025-02-07 17:38:06
 */
@Mapper
public interface LinkMapper extends BaseMapper<Link> {
    @Select("SELECT * FROM sg_link WHERE status = 0")
    List<Link> getAllLink();
}


