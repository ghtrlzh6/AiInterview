package com.aiinterview.dto.interview;

import lombok.Data;

@Data
public class SendMessageRequest {

    private String content;
    private String messageType = "NORMAL";
}
