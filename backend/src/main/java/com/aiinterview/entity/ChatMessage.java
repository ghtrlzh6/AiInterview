package com.aiinterview.entity;

import com.aiinterview.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_chat_message")
public class ChatMessage extends BaseEntity {

    private Long sessionId;
    private Long questionId;
    private String role;
    private String content;
    private String audioUrl;
    private String messageType;
    private Integer tokenCount;
}
