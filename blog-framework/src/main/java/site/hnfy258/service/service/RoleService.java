package site.hnfy258.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import site.hnfy258.entity.Role;

import java.util.List;


/**
 * 角色信息表(Role)表服务接口
 *
 * @author makejava
 * @since 2025-02-10 17:20:05
 */
public interface RoleService extends IService<Role> {

    List<String> selectRoleKeyByUserId(Long id);
}


