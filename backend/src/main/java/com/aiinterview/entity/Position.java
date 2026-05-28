package com.aiinterview.entity;

import com.aiinterview.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_position", autoResultMap = true)
public class Position extends BaseEntity {

    private String code;
    private String name;
    private String description;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> techStack;

    private String iconUrl;
    private Integer sortOrder;
    private Integer isActive;
}
