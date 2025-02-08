package site.hnfy258.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import site.hnfy258.entity.LoginUser;
import site.hnfy258.entity.User;
import site.hnfy258.mapper.UserMapper;
@Service

public class UserDetailsServiceImpl implements UserDetailsService {
    /**
     * @param s
     * @return
     * @throws UsernameNotFoundException
     */
    @Autowired
    private UserMapper userMapper;
    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        User user = userMapper.getUserByName(s);
        if(user.equals(null)){
            throw new RuntimeException("用户不存在");
        }

        return new LoginUser(user);
    }
}
