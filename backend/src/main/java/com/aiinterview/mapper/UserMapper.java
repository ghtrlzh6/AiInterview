package com.aiinterview.mapper;

import com.aiinterview.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM t_user WHERE username = #{username} AND is_deleted = 0 LIMIT 1")
    User findByUsername(String username);
}
