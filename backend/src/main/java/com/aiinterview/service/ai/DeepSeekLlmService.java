package com.aiinterview.service.ai;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.aiinterview.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeepSeekLlmService implements LlmService {

    private final SystemConfigService systemConfigService;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build();

    private static final String THINK_OPEN = "<think>";
    private static final String THINK_CLOSE = "</think>";

    @Override
    public boolean isAvailable() {
        return StringUtils.hasText(getApiKey());
    }

    @Override
    public String getModelName() {
        return systemConfigService.get("ai.llm.model", "deepseek-chat");
    }

    @Override
    public LlmTestResult testConnection() {
        long start = System.currentTimeMillis();
        if (!isAvailable()) {
            return new LlmTestResult(true, "mock", 5, "未配置 API Key，使用模拟模式");
        }
        try {
            JSONObject body = buildRequestBody(List.of(new ChatMessage("user", "ping")), false);
            Request request = new Request.Builder()
                    .url(getBaseUrl() + "/chat/completions")
                    .header("Authorization", "Bearer " + getApiKey())
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                    .build();
            try (Response response = client.newCall(request).execute()) {
                long latency = System.currentTimeMillis() - start;
                if (!response.isSuccessful() || response.body() == null) {
                    String err = response.body() != null ? response.body().string() : "empty body";
                    return new LlmTestResult(false, getModelName(), latency,
                            "LLM 连接失败（HTTP " + response.code() + "）: " + err);
                }
                JSONObject json = JSONUtil.parseObj(response.body().string());
                String content = stripReasoning(json.getByPath("choices[0].message.content", String.class));
                boolean ok = StringUtils.hasText(content);
                return new LlmTestResult(ok, getModelName(), latency,
                        ok ? "LLM 服务连接正常" : "LLM 返回空内容");
            }
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            log.warn("LLM connectivity test failed", e);
            return new LlmTestResult(false, getModelName(), latency, "LLM 连接异常: " + e.getMessage());
        }
    }

    @Override
    public String chat(List<ChatMessage> messages) {
        if (!isAvailable()) {
            return mockChatResponse(messages);
        }
        try {
            JSONObject body = buildRequestBody(messages, false);
            Request request = new Request.Builder()
                    .url(getBaseUrl() + "/chat/completions")
                    .header("Authorization", "Bearer " + getApiKey())
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    log.warn("LLM chat failed: {}", response.code());
                    return mockChatResponse(messages);
                }
                JSONObject json = JSONUtil.parseObj(response.body().string());
                return stripReasoning(json.getByPath("choices[0].message.content", String.class));
            }
        } catch (Exception e) {
            log.warn("LLM chat error, fallback to mock", e);
            return mockChatResponse(messages);
        }
    }

    @Override
    public void chatStream(List<ChatMessage> messages, Consumer<String> onToken) {
        if (!isAvailable()) {
            mockStream(mockChatResponse(messages), onToken);
            return;
        }
        try {
            JSONObject body = buildRequestBody(messages, true);
            Request request = new Request.Builder()
                    .url(getBaseUrl() + "/chat/completions")
                    .header("Authorization", "Bearer " + getApiKey())
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    mockStream(mockChatResponse(messages), onToken);
                    return;
                }
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8))) {
                    String line;
                    StringBuilder visibleBuffer = new StringBuilder();
                    boolean[] inReasoning = {false};
                    while ((line = reader.readLine()) != null) {
                        if (!line.startsWith("data: ")) {
                            continue;
                        }
                        String data = line.substring(6).trim();
                        if ("[DONE]".equals(data)) {
                            break;
                        }
                        JSONObject chunk = JSONUtil.parseObj(data);
                        String token = chunk.getByPath("choices[0].delta.content", String.class);
                        if (StringUtils.hasText(token)) {
                            String visible = filterReasoningToken(token, visibleBuffer, inReasoning);
                            if (StringUtils.hasText(visible)) {
                                onToken.accept(visible);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("LLM stream error, fallback to mock", e);
            mockStream(mockChatResponse(messages), onToken);
        }
    }

    @Override
    public List<Double> embed(String text) {
        if (!isAvailable()) {
            return mockEmbedding();
        }
        try {
            JSONObject body = new JSONObject();
            body.set("model", systemConfigService.get("ai.llm.embed-model", "deepseek-embed"));
            body.set("input", text);
            Request request = new Request.Builder()
                    .url(getBaseUrl() + "/embeddings")
                    .header("Authorization", "Bearer " + getApiKey())
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    return mockEmbedding();
                }
                JSONObject json = JSONUtil.parseObj(response.body().string());
                JSONArray arr = json.getByPath("data[0].embedding", JSONArray.class);
                List<Double> result = new ArrayList<>();
                if (arr != null) {
                    for (Object o : arr) {
                        result.add(((Number) o).doubleValue());
                    }
                }
                return result.isEmpty() ? mockEmbedding() : result;
            }
        } catch (IOException e) {
            log.warn("Embedding error, fallback", e);
            return mockEmbedding();
        }
    }

    private String getApiKey() {
        return systemConfigService.get("ai.llm.api-key", "");
    }

    private String getBaseUrl() {
        return systemConfigService.get("ai.llm.base-url", "https://api.deepseek.com/v1");
    }

    private int getMaxTokens() {
        return Integer.parseInt(systemConfigService.get("ai.llm.max-tokens", "4096"));
    }

    private double getTemperature() {
        return Double.parseDouble(systemConfigService.get("ai.llm.temperature", "0.7"));
    }

    private JSONObject buildRequestBody(List<ChatMessage> messages, boolean stream) {
        JSONObject body = new JSONObject();
        body.set("model", getModelName());
        body.set("max_tokens", getMaxTokens());
        body.set("temperature", getTemperature());
        body.set("stream", stream);
        JSONArray arr = new JSONArray();
        for (ChatMessage m : messages) {
            JSONObject item = new JSONObject();
            item.set("role", m.getRole());
            item.set("content", m.getContent());
            arr.add(item);
        }
        body.set("messages", arr);
        return body;
    }

    private String stripReasoning(String content) {
        if (!StringUtils.hasText(content)) {
            return content;
        }
        return content.replaceAll("(?is)<think>.*?</think>", "").trim();
    }

    private String filterReasoningToken(String token, StringBuilder buffer, boolean[] inReasoning) {
        buffer.append(token);
        StringBuilder visible = new StringBuilder();
        while (buffer.length() > 0) {
            if (inReasoning[0]) {
                int close = indexOfIgnoreCase(buffer, THINK_CLOSE);
                if (close < 0) {
                    trimBuffer(buffer, THINK_CLOSE.length());
                    return visible.toString();
                }
                buffer.delete(0, close + THINK_CLOSE.length());
                inReasoning[0] = false;
                continue;
            }

            int open = indexOfIgnoreCase(buffer, THINK_OPEN);
            if (open < 0) {
                int safeLength = Math.max(0, buffer.length() - THINK_OPEN.length() + 1);
                if (safeLength > 0) {
                    visible.append(buffer.substring(0, safeLength));
                    buffer.delete(0, safeLength);
                }
                return visible.toString();
            }

            visible.append(buffer.substring(0, open));
            buffer.delete(0, open + THINK_OPEN.length());
            inReasoning[0] = true;
        }
        return visible.toString();
    }

    private int indexOfIgnoreCase(StringBuilder text, String pattern) {
        return text.toString().toLowerCase().indexOf(pattern.toLowerCase());
    }

    private void trimBuffer(StringBuilder buffer, int keepChars) {
        if (buffer.length() > keepChars) {
            buffer.delete(0, buffer.length() - keepChars);
        }
    }

    private String mockChatResponse(List<ChatMessage> messages) {
        String lastUser = messages.stream()
                .filter(m -> "user".equalsIgnoreCase(m.getRole()))
                .reduce((a, b) -> b)
                .map(ChatMessage::getContent)
                .orElse("");
        if (lastUser.length() < 50) {
            return "{\"action\":\"follow_up\",\"content\":\"能再详细说明一下吗？比如结合实际例子或更具体的技术细节。\"}";
        }
        return "{\"action\":\"next_question\",\"content\":\"好的，你的回答我已了解。让我们继续下一题。\"}";
    }

    private void mockStream(String text, Consumer<String> onToken) {
        for (int i = 0; i < text.length(); i += 4) {
            onToken.accept(text.substring(i, Math.min(i + 4, text.length())));
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private List<Double> mockEmbedding() {
        List<Double> vec = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            vec.add(0.1 * i);
        }
        return vec;
    }
}
