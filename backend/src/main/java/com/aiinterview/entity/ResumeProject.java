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
@TableName(value = "t_resume_project", autoResultMap = true)
public class ResumeProject extends BaseEntity {

    private Long resumeId;
    private String projectName;
    private String summaryMd;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> techStackTokens;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> kbPointIdsHint;

    private Integer sortOrder;
}
