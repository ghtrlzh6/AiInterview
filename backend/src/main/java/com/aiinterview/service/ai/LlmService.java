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

    LlmTestResult testConnection();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class LlmTestResult {
        private boolean success;
        private String model;
        private long latencyMs;
        private String message;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class ChatMessage {
        private String role;
        private String content;
    }
}
