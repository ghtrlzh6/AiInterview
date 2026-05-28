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
@TableName(value = "t_question", autoResultMap = true)
public class Question extends BaseEntity {

    private String positionCode;
    private Long primaryKbModuleId;
    private Long codingChallengeId;
    private Long bindingSessionId;
    private String title;
    private String answerReference;
    private Integer difficulty;
    private String questionType;
    private String topic;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> followUpHints;

    private String source;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> generationMeta;

    private Integer sortOrder;
}
