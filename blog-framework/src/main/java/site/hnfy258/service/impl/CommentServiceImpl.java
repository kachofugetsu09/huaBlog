package site.hnfy258.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import site.hnfy258.entity.Article;
import site.hnfy258.DTO.NotificationMessage;
import site.hnfy258.Exception.SystemException;
import site.hnfy258.VO.CommentVo;
import site.hnfy258.VO.PageVo;
import site.hnfy258.config.RabbitMQConfig;
import site.hnfy258.constants.SystemConstants;
import site.hnfy258.entity.Comment;
import site.hnfy258.entity.User;
import site.hnfy258.enums.AppHttpCodeEnum;
import site.hnfy258.mapper.ArticleMapper;
import site.hnfy258.mapper.CommentMapper;
import site.hnfy258.service.CommentService;
import site.hnfy258.service.UserService;
import site.hnfy258.utils.BeanCopyUtils;

import java.util.List;

/**
 * 评论表(Comment)表服务实现类
 *
 * @author makejava
 * @since 2025-02-09 04:31:26
 */
@Slf4j
@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    @Autowired
    private UserService userService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ArticleMapper articleMapper;

    @Override
    public PageVo commentList(String commentType, Long articleId, Integer pageNum, Integer pageSize) {
        //查询对应文章的根评论
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        //对articleId进行判断
        queryWrapper.eq(SystemConstants.ARTICLE_COMMENT.equals(commentType),Comment::getArticleId,articleId);
        //根评论 rootId为-1
        queryWrapper.eq(Comment::getRootId,-1);

        //评论类型
        queryWrapper.eq(Comment::getType,commentType);

        //分页查询
        Page<Comment> page = new Page(pageNum,pageSize);
        page(page,queryWrapper);

        List<CommentVo> commentVoList = toCommentVoList(page.getRecords());

        //查询所有根评论对应的子评论集合，并且赋值给对应的属性
        for (CommentVo commentVo : commentVoList) {
            //查询对应的子评论
            List<CommentVo> children = getChildren(commentVo.getId());
            //赋值
            commentVo.setChildren(children);
        }

        return new PageVo(commentVoList,page.getTotal());
    }

    @Override
    public void addComment(Comment comment) {
        //评论内容不能为空
        if(!StringUtils.hasText(comment.getContent())){
            throw new SystemException(AppHttpCodeEnum.CONTENT_NOT_NULL);
        }
        save(comment);

        sendCommentNotification(comment);
    }

    private void sendCommentNotification(Comment comment) {
        try {
            // Don't proceed if the comment doesn't have a valid createBy
            if (comment.getCreateBy() == null) {
                log.warn("Cannot send notification: comment creator ID is null");
                return;
            }

            NotificationMessage notification = new NotificationMessage();
            notification.setCommentId(comment.getId());
            notification.setCommentType(comment.getType());

            // Get sender information
            Long userFromId = comment.getCreateBy();
            User userFrom = userService.getById(userFromId);

            // Check if the sender user exists
            if (userFrom == null) {
                log.warn("Cannot send notification: user with ID {} not found", userFromId);
                return;
            }

            // Set sender information in notification
            notification.setFromUserId(userFromId);
            notification.setFromUserAvatar(userFrom.getAvatar());
            notification.setFromUserNickName(userFrom.getNickName());
            notification.setArticleId(comment.getArticleId());
            notification.setCreateTime(comment.getCreateTime());

            // Determine the notification recipient
            Long toUserId = null;

            // Case 1: Comment is a root comment (direct comment on an article)
            if (comment.getRootId() == -1) {
                // Find article author if article ID exists
                if (comment.getArticleId() != null) {
                    Article article = articleMapper.getById(comment.getArticleId());
                    if (article != null) {
                        toUserId = article.getCreateBy();
                    } else {
                        log.warn("Cannot find article with ID: {}", comment.getArticleId());
                    }
                }
            }
            // Case 2: Comment is a reply to another comment
            else if (comment.getToCommentUserId() != null && comment.getToCommentUserId() != -1) {
                toUserId = comment.getToCommentUserId();
            }

            notification.setToUserId(toUserId);

            // Only send notification if:
            // 1. We have a valid recipient
            // 2. The recipient is not the same as the sender (don't notify yourself)
            if (toUserId != null && toUserId != -1L && !toUserId.equals(userFromId)) {
                // Verify the recipient exists
                User recipientUser = userService.getById(toUserId);
                if (recipientUser != null) {
                    rabbitTemplate.convertAndSend(
                        RabbitMQConfig.COMMENT_EXCHANGE, 
                        RabbitMQConfig.COMMENT_NOTIFICATION_ROUTING_KEY, 
                        notification
                    );
                    log.info("Sent comment notification from user {} to user {}", userFromId, toUserId);
                } else {
                    log.warn("Cannot send notification: recipient user with ID {} not found", toUserId);
                }
            }
        } catch (Exception e) {
            log.error("发送评论通知失败", e);
        }
    }

    /**
     * 根据根评论的id查询所对应的子评论的集合
     * @param id 根评论的id
     * @return
     */
    private List<CommentVo> getChildren(Long id) {

        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Comment::getRootId,id);
        queryWrapper.orderByAsc(Comment::getCreateTime);
        List<Comment> comments = list(queryWrapper);

        List<CommentVo> commentVos = toCommentVoList(comments);
        return commentVos;
    }

    private List<CommentVo> toCommentVoList(List<Comment> list) {
        List<CommentVo> commentVos = BeanCopyUtils.copyBeanList(list, CommentVo.class);
        // 遍历vo集合
        for (CommentVo commentVo : commentVos) {
            // 通过 createBy 查询用户的昵称和头像并赋值
            if (commentVo.getCreateBy() != null) {
                User user = userService.getById(commentVo.getCreateBy());
                if (user != null) {
                    commentVo.setUsername(user.getNickName());
                    commentVo.setAvatar(user.getAvatar() != null ? user.getAvatar() : "");
                }
            }
            // 通过 toCommentUserId 查询用户的昵称和头像并赋值
            if (commentVo.getToCommentUserId() != null && commentVo.getToCommentUserId() != -1) {
                User toCommentUser = userService.getById(commentVo.getToCommentUserId());
                if (toCommentUser != null) {
                    commentVo.setToCommentUserName(toCommentUser.getNickName());
                }
            }
        }
        return commentVos;
    }
}

