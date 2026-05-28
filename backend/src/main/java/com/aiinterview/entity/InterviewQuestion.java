package com.aiinterview.entity;

import com.aiinterview.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_interview_question")
public class InterviewQuestion extends BaseEntity {

    private Long sessionId;
    private Long questionId;
    private Integer questionOrder;
    private Integer isAnswered;
}
