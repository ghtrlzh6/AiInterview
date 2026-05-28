package com.aiinterview.entity;

import com.aiinterview.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_growth_record")
public class GrowthRecord extends BaseEntity {

    private Long userId;
    private Long reportId;
    private Long sessionId;
    private String positionCode;
    private BigDecimal overallScore;
    private BigDecimal techScore;
    private BigDecimal expressionScore;
    private BigDecimal logicScore;
    private BigDecimal depthScore;
    private BigDecimal confidenceScore;
    private LocalDate recordDate;
}
