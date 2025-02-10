package site.hnfy258.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import site.hnfy258.entity.Role;
import site.hnfy258.mapper.RoleMapper;
import site.hnfy258.service.RoleService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 角色信息表(Role)表服务实现类
 *
 * @author makejava
 * @since 2025-02-10 17:20:05
 */
@Service("roleService")
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    /**
     * @param id
     * @return
     */
    @Autowired
    private RoleMapper roleMapper;
    @Override
    public List<String> selectRoleKeyByUserId(Long id) {
        if(id == 1L){
            ArrayList<String> roleKeys=new ArrayList<>();
            roleKeys.add("admin");
            return roleKeys;
        }
        List<String> roleKeys =roleMapper.selectRoleKeyByUserId(id);
        return roleKeys;
    }
}


