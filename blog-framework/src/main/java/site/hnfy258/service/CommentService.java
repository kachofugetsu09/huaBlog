package site.hnfy258.service;

import com.baomidou.mybatisplus.extension.service.IService;
import site.hnfy258.VO.CommentVo;
import site.hnfy258.VO.PageVo;
import site.hnfy258.domain.ResponseResult;
import site.hnfy258.entity.Comment;


/**
 * 评论表(Comment)表服务接口
 *
 * @author makejava
 * @since 2025-02-09 04:31:26
 */
public interface CommentService extends IService<Comment> {

    PageVo commentList(String commentType,Long articleId, Integer pageNum, Integer pageSize);

    void addComment(Comment comment);
}

