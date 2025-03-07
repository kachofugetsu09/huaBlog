package site.hnfy258.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import site.hnfy258.entity.User;


/**
 * 用户表(User)表数据库访问层
 *
 * @author makejava
 * @since 2025-02-09 05:09:59
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    @Select("select * from sys_user where id = #{id}")
    User getUserById(Long id);
    @Select("select * from sys_user where user_name = #{userName}")
    User getUserByName(String s);
    @Select("select avatar from sys_user where id = #{id}")
    String getAvatarById(Long id);
    @Select("select nick_name from sys_user where id =#{otherUserId}")
    String getNicknameById(Long otherUserId);
}

