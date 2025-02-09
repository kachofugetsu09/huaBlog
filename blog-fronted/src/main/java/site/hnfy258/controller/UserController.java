package site.hnfy258.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.hnfy258.VO.UserInfoVo;
import site.hnfy258.domain.ResponseResult;
import site.hnfy258.service.UserService;
@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;

    @GetMapping("/userInfo")
    public ResponseResult<UserInfoVo> userInfo(){
        UserInfoVo userInfoVo = userService.getUserInfo();
        return ResponseResult.okResult(userInfoVo);
    }

}
