package site.hnfy258.service;

import com.baomidou.mybatisplus.extension.service.IService;
import site.hnfy258.VO.PageVo;
import site.hnfy258.VO.UserInfoVo;
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

    UserInfoVo getUserInfo();


    void updateUserInfo(User user);

    void register(User user);

    PageVo selectUserPage(User user, Integer pageNum, Integer pageSize);

    boolean checkUserNameUnique(String userName);

    boolean checkPhoneUnique(User user);

    boolean checkEmailUnique(User user);

    void addUser(User user);

    void updateUser(User user);


}

