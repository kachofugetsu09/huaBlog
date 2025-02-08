package site.hnfy258.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import site.hnfy258.VO.CommentVo;
import site.hnfy258.VO.PageVo;
import site.hnfy258.entity.Comment;
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
    @Override
    public boolean save(Comment entity) {
        return super.save(entity);
    }

    /**
     * @param articleId
     * @param pageNum
     * @param pageSize
     * @return
     */
    @Override
    public PageVo commentList(Long articleId, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum,pageSize);
        List<Comment> commentList =commentMapper.getRootComment(articleId);
        Page<Comment> page = (Page<Comment>) commentList;
        List<CommentVo> commentVoList = BeanCopyUtils.copyBeanList(page.getResult(), CommentVo.class);
        for(CommentVo commentVo:commentVoList){
            commentVo.setUsername(userService.getNickName(userService.getById(commentVo.getCreateBy())));
            if(commentVo.getRootId()!=-1){
                String toCommentUserName = userService.getNickName(userService.getById(commentVo.getToCommentUserId()));
                commentVo.setToCommentUserName(toCommentUserName);
            }
        }
        PageVo pageVo = new PageVo(commentVoList, page.getTotal());
        return pageVo;
    }
}

