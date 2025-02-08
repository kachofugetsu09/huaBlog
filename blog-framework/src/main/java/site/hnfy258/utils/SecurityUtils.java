package site.hnfy258.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import site.hnfy258.entity.LoginUser;

public class SecurityUtils
{

    /**
     * 获取用户
     **/
    public static LoginUser getLoginUser()
    {
        return (LoginUser) getAuthentication().getPrincipal();
    }

    /**
     * 获取Authentication
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public static Boolean isAdmin(){
        Long id = getLoginUser().getUser().getId();
        return id != null && 1L == id;
    }

    public static Long getUserId() {
        try {
            LoginUser loginUser = getLoginUser();
            if (loginUser != null && loginUser.getUser() != null) {
                return loginUser.getUser().getId();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1L;
    }
}