package com.aiinterview.entity;

import com.aiinterview.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_evaluation_report", autoResultMap = true)
public class EvaluationReport extends BaseEntity {

    private Long sessionId;
    private Long userId;
    private String positionCode;
    private String reportStatus;
    private BigDecimal overallScore;
    private BigDecimal techScore;
    private BigDecimal expressionScore;
    private BigDecimal logicScore;
    private BigDecimal depthScore;
    private BigDecimal confidenceScore;
    private String summary;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> highlights;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> weaknesses;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> suggestions;

    private String shareToken;
}
