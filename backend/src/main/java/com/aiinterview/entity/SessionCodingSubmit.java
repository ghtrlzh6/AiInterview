package com.aiinterview.entity;

import com.aiinterview.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_session_coding_submit")
public class SessionCodingSubmit extends BaseEntity {

    private Long sessionId;
    private Long questionId;
    private String codeBody;
    private String language;
    private Integer submitOrder;
}
