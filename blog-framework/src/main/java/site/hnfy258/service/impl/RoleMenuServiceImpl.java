package site.hnfy258.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.ibatis.annotations.Delete;
import org.springframework.stereotype.Service;
import site.hnfy258.entity.RoleMenu;
import site.hnfy258.mapper.RoleMenuMapper;
import site.hnfy258.service.RoleMenuService;

/**
 * 角色和菜单关联表(RoleMenu)表服务实现类
 *
 * @author huashen
 * @since 2025-02-11 16:32:06
 */
@Service("roleMenuService")
public class RoleMenuServiceImpl extends ServiceImpl<RoleMenuMapper, RoleMenu> implements RoleMenuService {

    /**
     * @param id
     */
    @Override
    @Delete("delete from sys_role_menu where role_id=#{id}")
    public void deleteRoleMenuByRoleId(Long id) {

    }
}

