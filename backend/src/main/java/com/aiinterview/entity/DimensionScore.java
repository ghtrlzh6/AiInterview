package com.aiinterview.entity;

import com.aiinterview.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_dimension_score")
public class DimensionScore extends BaseEntity {

    private Long reportId;
    private Long sessionId;
    private Long questionId;
    private Integer questionOrder;
    private BigDecimal techScore;
    private BigDecimal logicScore;
    private BigDecimal depthScore;
    private String comment;
}
