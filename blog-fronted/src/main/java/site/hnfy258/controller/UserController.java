package site.hnfy258.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import site.hnfy258.VO.UserInfoVo;
import site.hnfy258.annotation.SystemLog;
import site.hnfy258.domain.ResponseResult;
import site.hnfy258.entity.User;
import site.hnfy258.service.UserService;
@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;
    @SystemLog(bussinessName = "获取用户信息")
    @GetMapping("/userInfo")
    public ResponseResult<UserInfoVo> userInfo(){
        UserInfoVo userInfoVo = userService.getUserInfo();
        return ResponseResult.okResult(userInfoVo);
    }
    @SystemLog(bussinessName = "更新用户信息")
    @PutMapping("/userInfo")
    public ResponseResult updateUserInfo(@RequestBody User user){
        userService.updateUserInfo(user);
        return ResponseResult.okResult();
    }
    @SystemLog(bussinessName = "注册用户信息")
    @PostMapping("/register")
    public ResponseResult register(@RequestBody User user){
        userService.register(user);
        return ResponseResult.okResult();
    }

}
