package site.hnfy258.service;

import site.hnfy258.domain.ResponseResult;
import site.hnfy258.entity.User;

public interface LoginService {
    ResponseResult login(User user);

    void logout();
}