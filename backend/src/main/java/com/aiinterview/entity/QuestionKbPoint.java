package com.aiinterview.entity;

import com.aiinterview.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_question_kb_point")
public class QuestionKbPoint extends BaseEntity {

    private Long questionId;
    private Long kbNodeId;
    private BigDecimal relevanceWeight;
}
