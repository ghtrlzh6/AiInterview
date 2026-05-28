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
}
