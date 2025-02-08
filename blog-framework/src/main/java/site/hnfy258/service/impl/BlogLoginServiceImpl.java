package site.hnfy258.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import site.hnfy258.VO.BlogUserLoginVo;
import site.hnfy258.VO.UserInfoVo;
import site.hnfy258.domain.ResponseResult;
import site.hnfy258.entity.LoginUser;
import site.hnfy258.entity.User;
import site.hnfy258.service.BlogLoginService;
import site.hnfy258.utils.BeanCopyUtils;
import site.hnfy258.utils.JwtUtil;
import site.hnfy258.utils.RedisCache;

import java.util.Objects;

@Service

public class BlogLoginServiceImpl implements BlogLoginService {
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private RedisCache redisCache;

    /**
     * @param user
     * @return
     */
    @Override
    public BlogUserLoginVo login(User user) {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                new UsernamePasswordAuthenticationToken(user.getUserName(),user.getPassword());

        Authentication authenticate =
                authenticationManager.authenticate(usernamePasswordAuthenticationToken);

        if(Objects.isNull(authenticate)){
            throw new RuntimeException("用户名或密码错误");
        }

        LoginUser loginUser = (LoginUser) authenticate.getPrincipal();

        String userId = loginUser.getUser().getId().toString();
        redisCache.setCacheObject("bloglogin:"+userId,loginUser);

        String jwt = JwtUtil.createJWT(userId);
        UserInfoVo userinfovo = BeanCopyUtils.copyBean(loginUser.getUser(), UserInfoVo.class);
        BlogUserLoginVo blogUserLoginVo = new BlogUserLoginVo(jwt,userinfovo);

        return blogUserLoginVo;
    }

    /**
     *
     */
    @Override
    public void logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            LoginUser loginUser = (LoginUser) authentication.getPrincipal();
            Long userId = loginUser.getUser().getId();
            redisCache.deleteObject("bloglogin:" + userId);
    }
}
