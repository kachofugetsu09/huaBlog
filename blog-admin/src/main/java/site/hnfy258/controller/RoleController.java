package site.hnfy258.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import site.hnfy258.DTO.ChangeRoleStatusDto;
import site.hnfy258.VO.PageVo;
import site.hnfy258.domain.ResponseResult;
import site.hnfy258.entity.Role;
import site.hnfy258.service.RoleService;

import java.util.List;

@RestController
@RequestMapping("system/role")
public class RoleController {
    @Autowired
    private RoleService roleService;
    @GetMapping("/list")
    public ResponseResult<PageVo> list(Role role, Integer pageNum, Integer pageSize){
        PageVo pagevo = roleService.selectRolePage(role,pageNum,pageSize);
        return ResponseResult.okResult(pagevo);
    }

    @PutMapping("/changeStatus")
    public ResponseResult changeStatus(@RequestBody ChangeRoleStatusDto roleStatusDto){
        Role role = new Role();
        role.setId(roleStatusDto.getRoleId());
        role.setStatus(roleStatusDto.getStatus());
        return ResponseResult.okResult(roleService.updateById(role));
    }
    @PostMapping
    public ResponseResult add( @RequestBody Role role)
    {
        roleService.insertRole(role);
        return ResponseResult.okResult();

    }

    @GetMapping("/listAllRole")
    public ResponseResult listAllRole(){
        List<Role> roles = roleService.selectRoleAll();
        return ResponseResult.okResult(roles);
    }

    /**
     * 根据角色编号获取详细信息
     */
    @GetMapping(value = "/{roleId}")
    public ResponseResult getInfo(@PathVariable Long roleId)
    {
        Role role = roleService.getById(roleId);
        return ResponseResult.okResult(role);
    }

    /**
     * 修改保存角色
     */
    @PutMapping
    public ResponseResult edit(@RequestBody Role role)
    {
        roleService.updateRole(role);
        return ResponseResult.okResult();
    }

    /**
     * 删除角色
     * @param id
     */
    @DeleteMapping("/{id}")
    public ResponseResult remove(@PathVariable(name = "id") Long id) {
        roleService.removeById(id);
        return ResponseResult.okResult();
    }

}
