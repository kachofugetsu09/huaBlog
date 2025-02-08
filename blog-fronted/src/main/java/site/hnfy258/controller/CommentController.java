package site.hnfy258.controller;

import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.hnfy258.VO.CommentVo;
import site.hnfy258.VO.PageVo;
import site.hnfy258.domain.ResponseResult;
import site.hnfy258.service.CommentService;

@RestController
@RequestMapping("/comment")
@Api(tags = "评论")
public class CommentController {
    @Autowired
    private CommentService commentService;;
    @GetMapping("/commentList")
    public ResponseResult<CommentVo> commentList(
            Long articleId,
            Integer pageNum,
            Integer pageSize
    ){
        PageVo pageVo = commentService.commentList(articleId,pageNum,pageSize);
        return ResponseResult.okResult(pageVo);
    }
}
