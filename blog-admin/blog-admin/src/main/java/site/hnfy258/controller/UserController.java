package site.hnfy258.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import site.hnfy258.Exception.SystemException;
import site.hnfy258.VO.PageVo;
import site.hnfy258.domain.ResponseResult;
import site.hnfy258.entity.User;
import site.hnfy258.enums.AppHttpCodeEnum;
import site.hnfy258.service.UserService;
import site.hnfy258.service.impl.UserServiceImpl;
import site.hnfy258.utils.SecurityUtils;

import java.util.List;

@RestController
@RequestMapping("/system/user")

public class UserController {
        @Autowired
        private UserService userService;

    @ApiOperation("用户列表")
    @GetMapping("/list")
    public ResponseResult<PageVo> list(User user, Integer pageNum, Integer pageSize){
        PageVo pageVo = userService.selectUserPage(user,pageNum,pageSize);
        return ResponseResult.okResult(pageVo);
    }
    @PostMapping
    @ApiOperation("新增用户")
    public ResponseResult add(@RequestBody User user){
        if(!StringUtils.hasText(user.getUserName())){
            throw new SystemException(AppHttpCodeEnum.REQUIRE_USERNAME);
        }
        if (!userService.checkUserNameUnique(user.getUserName())){
            throw new SystemException(AppHttpCodeEnum.USERNAME_EXIST);
        }
        if (!userService.checkPhoneUnique(user)){
            throw new SystemException(AppHttpCodeEnum.PHONENUMBER_EXIST);
        }
        if (!userService.checkEmailUnique(user)){
            throw new SystemException(AppHttpCodeEnum.EMAIL_EXIST);
        }
        userService.addUser(user);
        return ResponseResult.okResult();
    }

    @PutMapping
    public ResponseResult edit(@RequestBody User user) {
        userService.updateUser(user);
        return ResponseResult.okResult();
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{userIds}")
    public ResponseResult remove(@PathVariable List<Long> userIds) {
        if(userIds.contains(SecurityUtils.getUserId())){
            return ResponseResult.errorResult(500,"不能删除当前你正在使用的用户");
        }
        userService.removeByIds(userIds);
        return ResponseResult.okResult();
    }
}
