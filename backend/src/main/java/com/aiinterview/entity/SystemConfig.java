package com.aiinterview.entity;

import com.aiinterview.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_system_config")
public class SystemConfig extends BaseEntity {

    private String configKey;
    private String configValue;
    private String configType;
    private String description;
    private Integer isSensitive;
}
