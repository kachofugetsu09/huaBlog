package site.hnfy258.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import site.hnfy258.entity.LoginUser;

public class SecurityUtils
{


    /**
     * 获取Authentication
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
    public static LoginUser getLoginUser()
    {
        Authentication authentication = getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            // Handle the case where the user is not authenticated (e.g., anonymous or not logged in)
            return null; // Or throw an exception if required
        }

        Object principal = authentication.getPrincipal();

        // Check if the principal is of the expected type (LoginUser)
        if (principal instanceof LoginUser) {
            return (LoginUser) principal;
        }

        return null; // Return null or handle as appropriate if the principal is not a LoginUser
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
            System.out.println("获取用户ID异常");
        }
        return -1L;
    }
}