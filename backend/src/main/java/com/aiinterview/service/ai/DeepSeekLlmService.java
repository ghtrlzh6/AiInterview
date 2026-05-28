package com.aiinterview.service.ai;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
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
public class DeepSeekLlmService implements LlmService {

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build();

    @Value("${ai.llm.api-key:}")
    private String apiKey;

    @Value("${ai.llm.base-url:https://api.deepseek.com/v1}")
    private String baseUrl;

    @Value("${ai.llm.model:deepseek-chat}")
    private String model;

    @Value("${ai.llm.max-tokens:4096}")
    private int maxTokens;

    @Value("${ai.llm.temperature:0.7}")
    private double temperature;

    @Override
    public boolean isAvailable() {
        return StringUtils.hasText(apiKey);
    }

    @Override
    public String getModelName() {
        return model;
    }

    @Override
    public String chat(List<ChatMessage> messages) {
        if (!isAvailable()) {
            return mockChatResponse(messages);
        }
        try {
            JSONObject body = buildRequestBody(messages, false);
            Request request = new Request.Builder()
                    .url(baseUrl + "/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    log.warn("LLM chat failed: {}", response.code());
                    return mockChatResponse(messages);
                }
                JSONObject json = JSONUtil.parseObj(response.body().string());
                return json.getByPath("choices[0].message.content", String.class);
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
                    .url(baseUrl + "/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
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
                            onToken.accept(token);
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
            body.set("model", "deepseek-embed");
            body.set("input", text);
            Request request = new Request.Builder()
                    .url(baseUrl + "/embeddings")
                    .header("Authorization", "Bearer " + apiKey)
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

    private JSONObject buildRequestBody(List<ChatMessage> messages, boolean stream) {
        JSONObject body = new JSONObject();
        body.set("model", model);
        body.set("max_tokens", maxTokens);
        body.set("temperature", temperature);
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
