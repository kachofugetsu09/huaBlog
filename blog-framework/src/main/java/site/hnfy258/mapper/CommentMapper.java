package site.hnfy258.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import site.hnfy258.VO.CommentVo;
import site.hnfy258.entity.Comment;

import java.util.List;


/**
 * 评论表(Comment)表数据库访问层
 *
 * @author makejava
 * @since 2025-02-09 04:31:26
 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    List<Comment> getRootComment(@Param("articleId") Long articleId, @Param("commentType") String commentType);
    List<Comment> getChildren(@Param("articleId") Long articleId, @Param("id") Long rootId, @Param("commentType") String commentType);
}

