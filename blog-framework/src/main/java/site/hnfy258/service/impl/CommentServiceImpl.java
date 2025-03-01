package site.hnfy258.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import site.hnfy258.Exception.SystemException;
import site.hnfy258.VO.CommentVo;
import site.hnfy258.VO.PageVo;
import site.hnfy258.domain.ResponseResult;
import site.hnfy258.entity.Comment;
import site.hnfy258.entity.User;
import site.hnfy258.enums.AppHttpCodeEnum;
import site.hnfy258.mapper.CommentMapper;
import site.hnfy258.service.CommentService;
import site.hnfy258.service.UserService;
import site.hnfy258.utils.BeanCopyUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 评论表(Comment)表服务实现类
 *
 * @author makejava
 * @since 2025-02-09 04:31:26
 */
@Service("commentService")
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private UserService userService;

    /**
     * @param entity
     * @return
     */


    /**
     * @param articleId
     * @param pageNum
     * @param pageSize
     * @return
     */
    @Override
    public PageVo commentList(String commentType, Long articleId, Integer pageNum, Integer pageSize) {
        // 分页查询根评论
        PageHelper.startPage(pageNum, pageSize);
        List<Comment> rootCommentList = commentMapper.getRootComment(articleId, commentType);
        Page<Comment> page = (Page<Comment>) rootCommentList;

        // 转换为 CommentVo
        List<CommentVo> commentVoList = BeanCopyUtils.copyBeanList(page.getResult(), CommentVo.class);

        // 设置用户名和子评论
        for (CommentVo commentVo : commentVoList) {
            // 设置用户名
            commentVo.setUsername(userService.getNickName(userService.getById(commentVo.getCreateBy())));

            // 查询子评论
            List<Comment> childrenCommentList = commentMapper.getChildren(articleId, commentVo.getId(), commentType);
            List<CommentVo> childrenCommentVoList = BeanCopyUtils.copyBeanList(childrenCommentList, CommentVo.class);

            // 设置子评论的用户名
            for (CommentVo childCommentVo : childrenCommentVoList) {
                childCommentVo.setUsername(userService.getNickName(userService.getById(childCommentVo.getCreateBy())));
                if (childCommentVo.getToCommentUserId() != null) {
                    String toCommentUserName = userService.getNickName(userService.getById(childCommentVo.getToCommentUserId()));
                    childCommentVo.setToCommentUserName(toCommentUserName);
                }
            }

            // 设置子评论
            commentVo.setChildren(childrenCommentVoList);
        }

        // 返回分页结果
        return new PageVo(commentVoList, page.getTotal());
    }


    /**
     * @param comment
     * @return
     */
    @Override
    public void addComment(Comment comment) {
        if(comment.getContent() == null){
            throw new SystemException(AppHttpCodeEnum.CONTENT_NOT_NULL);
        }

        save(comment);
    }
}

