package site.hnfy258.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import site.hnfy258.VO.BlogUserLoginVo;
import site.hnfy258.domain.ResponseResult;
import site.hnfy258.entity.User;
import site.hnfy258.service.BlogLoginService;

@RestController
public class BlogLoginController {
    @Autowired
    private BlogLoginService blogLoginService;

    @PostMapping("/login")
    public ResponseResult<BlogUserLoginVo> login(@RequestBody User user){
       BlogUserLoginVo blogUserLoginVo = blogLoginService.login(user);
       return ResponseResult.okResult(blogUserLoginVo);
    }
    @PostMapping("/logout")
    public ResponseResult logout(){
        blogLoginService.logout();
        return ResponseResult.okResult() ;
    }
}
