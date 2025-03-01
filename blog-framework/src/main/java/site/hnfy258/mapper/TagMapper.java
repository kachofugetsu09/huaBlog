package site.hnfy258.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import site.hnfy258.entity.Tag;

import java.util.List;


/**
 * 标签(Tag)表数据库访问层
 *
 * @author makejava
 * @since 2025-02-10 16:14:02
 */
public interface TagMapper extends BaseMapper<Tag> {


    List<Tag> selectTagList(@Param("name") String name,    // 必须添加@Param注解
                            @Param("remark") String remark);
}


