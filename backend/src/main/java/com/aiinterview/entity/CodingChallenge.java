package com.aiinterview.entity;

import com.aiinterview.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_coding_challenge", autoResultMap = true)
public class CodingChallenge extends BaseEntity {

    private String externalRef;
    private String title;
    private String problemMd;
    private Integer difficulty;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> canonicalTags;

    private String answerHintMd;
    private Integer isActive;

    /** 评判配置：testCases / inputFormat / outputFormat / timeLimit */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> judgeConfig;

    /** 各语言起始代码模板 {java, python, cpp, javascript} */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, String> starterCode;
}
