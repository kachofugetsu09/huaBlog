package site.hnfy258.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import site.hnfy258.VO.UserInfoVo;
import site.hnfy258.entity.User;
import site.hnfy258.mapper.UserMapper;
import site.hnfy258.service.UserService;
import site.hnfy258.utils.BeanCopyUtils;
import site.hnfy258.utils.SecurityUtils;

/**
 * 用户表(User)表服务实现类
 *
 * @author makejava
 * @since 2025-02-09 05:09:58
 */
@Service("userService")
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    UserMapper userMapper;


    @Override
    public User getById(Long id) {
        return userMapper.getUserById(id);
    }

    /**
     * @param user
     * @return
     */
    @Override
    public String getNickName(User user) {
        return user.getNickName();
    }

    /**
     * @return
     */
    @Override
    public UserInfoVo getUserInfo() {
        Long userId = SecurityUtils.getUserId();
        User user = getById(userId);
        UserInfoVo userInfoVo = BeanCopyUtils.copyBean(user, UserInfoVo.class);
        return userInfoVo;
    }
}

