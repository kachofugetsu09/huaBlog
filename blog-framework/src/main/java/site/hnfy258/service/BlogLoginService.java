package site.hnfy258.service;

import site.hnfy258.VO.BlogUserLoginVo;
import site.hnfy258.domain.ResponseResult;
import site.hnfy258.entity.User;

public interface BlogLoginService {
    BlogUserLoginVo login(User user);

    void logout();
}
