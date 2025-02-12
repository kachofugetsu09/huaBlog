package site.hnfy258.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.hnfy258.Exception.SystemException;
import site.hnfy258.VO.PageVo;
import site.hnfy258.VO.UserInfoVo;
import site.hnfy258.VO.UserVo;
import site.hnfy258.entity.User;
import site.hnfy258.entity.UserRole;
import site.hnfy258.enums.AppHttpCodeEnum;
import site.hnfy258.mapper.UserMapper;
import site.hnfy258.service.UserRoleService;
import site.hnfy258.service.UserService;
import site.hnfy258.utils.BeanCopyUtils;
import site.hnfy258.utils.SecurityUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RoleServiceImpl roleService;
    @Autowired
    private UserRoleService userRoleService;


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

    /**
     * @param user
     */
    @Override
    public void updateUserInfo(User user) {
        updateById(user);
    }

    /**
     * @param user
     */
    @Override
    public void register(User user) {
        if(user.getUserName()==null){
            throw new SystemException(AppHttpCodeEnum.USERNAME_NOT_NULL);
        }
        if(user.getNickName()==null){
            throw new SystemException(AppHttpCodeEnum.NICKNAME_NOT_NULL);
        }
        if(user.getPassword()==null){
            throw new SystemException(AppHttpCodeEnum.PASSWORD_NOT_NULL);
        }
        if(user.getEmail()==null){
            throw new SystemException(AppHttpCodeEnum.EMAIL_NOT_NULL);
        }
        String encodePassWord = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodePassWord);
        save(user);
    }

    /**
     * @param user
     * @param pageNum
     * @param pageSize
     * @return
     */
    @Override
    public PageVo selectUserPage(User user, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper();
        queryWrapper.like(user.getUserName()!=null,User::getUserName,user.getUserName());
        queryWrapper.eq(user.getPhonenumber()!=null,User::getPhonenumber,user.getPhonenumber());
        queryWrapper.eq(user.getStatus()!=null,User::getStatus,user.getStatus());

        Page<User> page = new Page<>();
        page(page,queryWrapper);

        List<User> userList = page.getRecords();
        List<UserVo> userVoList = BeanCopyUtils.copyBeanList(userList, UserVo.class);
        PageVo pageVo = new PageVo(userVoList,page.getTotal());


        return pageVo;
    }
    @Override
    public boolean checkUserNameUnique(String userName) {
        return count(Wrappers.<User>lambdaQuery().eq(User::getUserName,userName))==0;
    }

    @Override
    public boolean checkPhoneUnique(User user) {
        return count(Wrappers.<User>lambdaQuery().eq(User::getPhonenumber,user.getPhonenumber()))==0;
    }

    @Override
    public boolean checkEmailUnique(User user) {
        return count(Wrappers.<User>lambdaQuery().eq(User::getEmail,user.getEmail()))==0;
    }

    /**
     * @param user
     */
    @Transactional
    @Override
    public void addUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        save(user);

        if(user.getRoleIds()!=null&&user.getRoleIds().length>0){
            insertUserRole(user);
        }
    }

    /**
     * @param user
     */
    @Override
    public void updateUser(User user) {
        // 删除用户与角色关联
        LambdaQueryWrapper<UserRole> userRoleUpdateWrapper = new LambdaQueryWrapper<>();
        userRoleUpdateWrapper.eq(UserRole::getUserId,user.getId());
        userRoleService.remove(userRoleUpdateWrapper);

        // 新增用户与角色管理
        insertUserRole(user);
        // 更新用户信息
        updateById(user);
    }


    public void insertUserRole(User user){
        List<UserRole> userRoleList = Arrays.stream(user.getRoleIds()).
                map(roleId-> new UserRole(user.getId(),roleId)).collect(Collectors.toList());
        userRoleService.saveBatch(userRoleList);
    }


}

