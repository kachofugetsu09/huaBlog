package site.hnfy258.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import site.hnfy258.entity.RoleMenu;


/**
 * 角色和菜单关联表(RoleMenu)表数据库访问层
 *
 * @author huashen
 * @since 2025-02-11 16:32:05
 */
@Mapper
public interface RoleMenuMapper extends BaseMapper<RoleMenu> {

}

