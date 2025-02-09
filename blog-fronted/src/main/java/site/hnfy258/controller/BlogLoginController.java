package site.hnfy258.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import site.hnfy258.Exception.SystemException;
import site.hnfy258.VO.BlogUserLoginVo;
import site.hnfy258.annotation.SystemLog;
import site.hnfy258.domain.ResponseResult;
import site.hnfy258.entity.User;
import site.hnfy258.enums.AppHttpCodeEnum;
import site.hnfy258.service.BlogLoginService;

@RestController
public class BlogLoginController {
    @Autowired
    private BlogLoginService blogLoginService;
    @SystemLog(bussinessName = "用户登录")
    @PostMapping("/login")
    public ResponseResult<BlogUserLoginVo> login(@RequestBody User user){
        if(!StringUtils.hasText(user.getUserName())){
            //提示 必须要传用户名
            throw new SystemException(AppHttpCodeEnum.REQUIRE_USERNAME);
        }
       BlogUserLoginVo blogUserLoginVo = blogLoginService.login(user);
       return ResponseResult.okResult(blogUserLoginVo);
    }
    @SystemLog(bussinessName = "用户登出")
    @PostMapping("/logout")
    public ResponseResult logout(){
        blogLoginService.logout();
        return ResponseResult.okResult() ;
    }
}
