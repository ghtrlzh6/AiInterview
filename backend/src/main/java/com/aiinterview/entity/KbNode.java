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
@TableName(value = "t_kb_node", autoResultMap = true)
public class KbNode extends BaseEntity {

    private Long parentId;
    private String title;
    private String slug;
    private String codePath;
    private Integer depth;
    private Integer sortOrder;
    private String nodeType;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> positionCodes;

    private String summaryExcerpt;
    private Integer isActive;
}
