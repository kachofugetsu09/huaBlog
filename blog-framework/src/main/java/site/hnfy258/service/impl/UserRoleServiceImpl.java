package site.hnfy258.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import site.hnfy258.entity.UserRole;
import site.hnfy258.mapper.UserRoleMapper;
import site.hnfy258.service.UserRoleService;

/**
 * 用户和角色关联表(UserRole)表服务实现类
 *
 * @author makejava
 * @since 2025-02-12 19:59:26
 */
@Service("userRoleService")
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole> implements UserRoleService {

}

