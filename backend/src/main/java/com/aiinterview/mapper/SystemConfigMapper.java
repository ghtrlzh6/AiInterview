package com.aiinterview.mapper;

import com.aiinterview.entity.SystemConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfig> {

    @Select("SELECT * FROM t_system_config WHERE config_key = #{key} AND is_deleted = 0 LIMIT 1")
    SystemConfig findByKey(String key);
}
