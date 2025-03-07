package site.hnfy258.controller;

import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import site.hnfy258.VO.CommentVo;
import site.hnfy258.VO.PageVo;
import site.hnfy258.annotation.SystemLog;
import site.hnfy258.constants.SystemConstants;
import site.hnfy258.domain.ResponseResult;
import site.hnfy258.entity.Comment;
import site.hnfy258.service.CommentService;

@RestController
@RequestMapping("/comment")
@Api(tags = "评论")
public class CommentController {
    @Autowired
    private CommentService commentService;
    ;
    @SystemLog(bussinessName = "显示评论列表")
    @GetMapping("/commentList")
    public ResponseResult<CommentVo> commentList(
            Long articleId,
            Integer pageNum,
            Integer pageSize
    ) {
        PageVo pageVo = commentService.commentList(SystemConstants.ARTICLE_COMMENT, articleId, pageNum, pageSize);
        return ResponseResult.okResult(pageVo);
    }
    @SystemLog(bussinessName = "添加评论信息")
    @PostMapping("/addComment")
    public ResponseResult addComment(@RequestBody Comment comment) {
        commentService.addComment(comment);
        return ResponseResult.okResult();
    }
    @SystemLog(bussinessName = "显示友链评论列表")
    @GetMapping("/linkCommentList")
    public ResponseResult linkCommentList(Integer pageNum, Integer pageSize) {
        PageVo pageVo= commentService.commentList(SystemConstants.LINK_COMMENT, null, pageNum, pageSize);
    return ResponseResult.okResult(pageVo);
    }
}
