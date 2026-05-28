package com.aiinterview.service.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.function.Consumer;

public interface LlmService {

    String chat(List<ChatMessage> messages);

    void chatStream(List<ChatMessage> messages, Consumer<String> onToken);

    List<Double> embed(String text);

    boolean isAvailable();

    String getModelName();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class ChatMessage {
        private String role;
        private String content;
    }
}
