package site.hnfy258.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import site.hnfy258.VO.PageVo;
import site.hnfy258.constants.SystemConstants;
import site.hnfy258.entity.Role;
import site.hnfy258.entity.RoleMenu;
import site.hnfy258.entity.User;
import site.hnfy258.mapper.RoleMapper;
import site.hnfy258.service.RoleService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

    @Autowired
    private RoleMenuServiceImpl roleMenuService;
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

    /**
     * @param role
     * @param pageNum
     * @param pageSize
     * @return
     */
    @Override
    public PageVo selectRolePage(Role role, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<Role> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        //目前没有根据id查询
//        lambdaQueryWrapper.eq(Objects.nonNull(role.getId()),Role::getId,role.getId());
        lambdaQueryWrapper.like(StringUtils.hasText(role.getRoleName()),Role::getRoleName,role.getRoleName());
        lambdaQueryWrapper.eq(StringUtils.hasText(role.getStatus()),Role::getStatus,role.getStatus());
        lambdaQueryWrapper.orderByAsc(Role::getRoleSort);

        Page<Role> page = new Page<>();
        page.setCurrent(pageNum);
        page.setSize(pageSize);
        page(page,lambdaQueryWrapper);

        //转换成VO
        List<Role> roles = page.getRecords();

        PageVo pageVo = new PageVo();
        pageVo.setTotal(page.getTotal());
        pageVo.setRows(roles);
        return pageVo;
    }

    /**
     * @param role
     */
    @Transactional
    @Override
    public void insertRole(Role role) {
        save(role);
        if(role.getMenuIds()!=null&&role.getMenuIds().length>0){
            insertRoleMenu(role);
        }

    }

    @Override
    public void updateRole(Role role) {
        updateById(role);
        roleMenuService.deleteRoleMenuByRoleId(role.getId());
        insertRoleMenu(role);
    }

    private void insertRoleMenu(Role role){
        List<RoleMenu> roleMenuList= Arrays.stream(role.getMenuIds()).map(menuId-> new RoleMenu(role.getId(),menuId)).collect(Collectors.toList());
        roleMenuService.saveBatch(roleMenuList);
    }

    @Override
    public List<Role> selectRoleAll() {
        return list(Wrappers.<Role>lambdaQuery().eq(Role::getStatus, SystemConstants.NORMAL));
    }

    @Override
    public List<Long> selectRoleIdByUserId(Long userId) {
        return getBaseMapper().selectRoleIdByUserId(userId);
    }

    /**
     * @param user
     */
}


