package site.hnfy258.service;

import com.baomidou.mybatisplus.extension.service.IService;
import site.hnfy258.entity.User;


/**
 * 用户表(User)表服务接口
 *
 * @author makejava
 * @since 2025-02-09 05:09:57
 */
public interface UserService extends IService<User> {
    User getById(Long id);
    String getNickName(User user);

}

