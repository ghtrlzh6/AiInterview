package com.aiinterview.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.ByteString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 讯飞语音识别服务
 * 使用WebSocket协议与讯飞实时语音转写API通信
 */
@Slf4j
@Service
public class XunfeiAsrService {

    @Value("${ai.xunfei.app-id:}")
    private String appId;

    @Value("${ai.xunfei.api-key:}")
    private String apiKey;

    @Value("${ai.xunfei.api-secret:}")
    private String apiSecret;

    @Value("${ai.xunfei.asr-url:wss://rtasr.xfyun.cn/v1/ws}")
    private String asrUrl;

    @Value("${ai.xunfei.enabled:false}")
    private boolean enabled;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 音频文件转文字
     * @param audioData 音频数据（PCM格式）
     * @param format 音频格式 (pcm, wav)
     * @param sampleRate 采样率 (16000, 8000)
     * @return 识别结果
     */
    public AsrResult convertAudio(byte[] audioData, String format, int sampleRate) {
        // 检查配置是否完整
        if (!enabled || 
            apiKey == null || apiKey.isEmpty() || 
            apiSecret == null || apiSecret.isEmpty() || 
            appId == null || appId.isEmpty()) {
            log.warn("讯飞ASR未启用或未配置完整API参数，使用模拟模式");
            return getMockResult();
        }

        // 检查音频数据
        if (audioData == null || audioData.length == 0) {
            log.warn("音频数据为空，使用模拟模式");
            return getMockResult();
        }

        try {
            return callRealXunfeiApi(audioData, format, sampleRate);
        } catch (Exception e) {
            log.error("讯飞ASR调用失败: {}", e.getMessage());
            // 降级返回模拟结果
            return getMockResult();
        }
    }

    /**
     * 调用真实讯飞ASR WebSocket API
     */
    private AsrResult callRealXunfeiApi(byte[] audioData, String format, int sampleRate) throws Exception {
        // 使用UTC时间戳（讯飞API要求使用UNIX时间戳，秒级）
        // 必须是final以便在匿名内部类中使用
        final long ts = java.time.Instant.now().getEpochSecond();
        // RFC 1123格式的日期用于签名计算
        final String dateStr = java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME.format(java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC));
        String authUrl = buildAuthUrl(dateStr, ts);
        log.info("讯飞ASR请求URL: {}", authUrl);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> resultText = new AtomicReference<>("");
        AtomicReference<Boolean> isError = new AtomicReference<>(false);

        Request request = new Request.Builder().url(authUrl).build();

        WebSocket webSocket = httpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                log.info("WebSocket连接已建立");
                try {
                    // 发送初始化参数，ts使用数字格式（Unix时间戳）
                    String initParams = String.format(
                        "{\"common\":{\"app_id\":\"%s\",\"ts\":%d},\"business\":{\"language\":\"zh_cn\",\"domain\":\"iat\",\"accent\":\"mandarin\",\"sample_rate\":%d},\"data\":{\"status\":0,\"format\":\"audio/L16;rate=%d\",\"encoding\":\"raw\"}}",
                        appId, ts, sampleRate, sampleRate
                    );
                    // 详细日志
                    log.info("发送初始化参数 - appId: {}, ts: {}, sampleRate: {}", appId, ts, sampleRate);
                    log.info("初始化参数JSON: {}", initParams);
                    webSocket.send(initParams);
                } catch (Exception e) {
                    log.error("发送初始化参数失败: {}", e.getMessage());
                    isError.set(true);
                    latch.countDown();
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                log.debug("收到消息: {}", text);
                try {
                    // 使用Jackson解析JSON响应
                    JsonNode root = objectMapper.readTree(text);
                    
                    // 检查错误码
                    if (root.has("code") && root.get("code").asInt() == 0) {
                        // 成功响应，提取识别结果
                        if (root.has("data") && root.get("data").isTextual()) {
                            String data = root.get("data").asText();
                            try {
                                String decoded = new String(Base64.getDecoder().decode(data), StandardCharsets.UTF_8);
                                log.info("识别结果: {}", decoded);
                                // 累积识别结果
                                resultText.set(resultText.get() + decoded);
                            } catch (Exception e) {
                                log.warn("解析识别结果失败: {}", e.getMessage());
                            }
                        }
                        
                        // 检查是否是最后一条消息
                        if (root.has("data") && root.get("data").has("status") && 
                            root.get("data").get("status").asInt() == 2) {
                            webSocket.close(1000, "识别完成");
                        }
                    } else {
                        String errorMsg = root.has("message") ? root.get("message").asText() : "未知错误";
                        log.error("讯飞ASR错误响应: code={}, message={}", 
                            root.has("code") ? root.get("code").asInt() : -1, errorMsg);
                        isError.set(true);
                        webSocket.close(1001, "错误: " + errorMsg);
                    }
                } catch (Exception e) {
                    log.error("解析响应失败: {}", e.getMessage());
                    isError.set(true);
                    webSocket.close(1001, "解析错误");
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                log.debug("收到二进制消息，长度: {}", bytes.size());
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                log.info("WebSocket关闭中: code={}, reason={}", code, reason);
                latch.countDown();
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                log.info("WebSocket已关闭: code={}, reason={}", code, reason);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                log.error("WebSocket连接失败: {}", t.getMessage());
                isError.set(true);
                latch.countDown();
            }
        });

        // 分块发送音频数据（每帧20ms，16000采样率，单声道，16位=2字节）
        int frameSize = sampleRate * 2 * 20 / 1000; // 640 bytes for 16000Hz
        
        for (int i = 0; i < audioData.length; i += frameSize) {
            int end = Math.min(i + frameSize, audioData.length);
            byte[] frame = new byte[end - i];
            System.arraycopy(audioData, i, frame, 0, frame.length);
            webSocket.send(ByteString.of(frame));
            
            // 控制发送速率，避免过快
            if (i + frameSize < audioData.length) {
                Thread.sleep(15);
            }
        }

        // 发送结束标志
        String endSignal = "{\"data\":{\"status\":2}}";
        webSocket.send(endSignal);

        // 等待识别完成
        boolean completed = latch.await(30, TimeUnit.SECONDS);

        if (!completed) {
            log.warn("讯飞ASR超时");
            try {
                webSocket.close(1001, "超时");
            } catch (Exception e) {
                log.warn("关闭WebSocket失败: {}", e.getMessage());
            }
            return getMockResult();
        }

        AsrResult result = new AsrResult();
        if (isError.get() || resultText.get().isEmpty()) {
            result.setText("未识别到语音");
            result.setSuccess(false);
            result.setMock(true);
            result.setConfidence(0.0);
        } else {
            result.setText(resultText.get());
            result.setSuccess(true);
            result.setMock(false);
            result.setConfidence(0.95);
        }
        result.setDuration((double) audioData.length / (sampleRate * 2));

        return result;
    }

    /**
     * 构建WebSocket鉴权URL
     * @param dateStr RFC 1123格式的日期字符串
     * @param ts Unix时间戳（秒级）
     */
    private String buildAuthUrl(String dateStr, long ts) throws Exception {
        String host = "rtasr.xfyun.cn";
        String tsStr = String.valueOf(ts);
        // 严格按照讯飞API要求的格式构建签名字符串（date使用RFC 1123格式）
        String signStr = "host: " + host + "\n" 
                      + "date: " + dateStr + "\n" 
                      + "GET /v1/ws HTTP/1.1";
        
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(keySpec);
        byte[] hash = mac.doFinal(signStr.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getEncoder().encodeToString(hash);
        
        // 构建authorization字符串，注意格式细节
        String authorization = String.format("api_key=\"%s\", algorithm=\"hmac-sha256\", headers=\"host date request-line\", signature=\"%s\"", 
                                           apiKey, signature);
        authorization = URLEncoder.encode(authorization, StandardCharsets.UTF_8);
        
        // 构建完整的WebSocket URL（必须包含 ts 参数）
        String url = String.format("wss://%s/v1/ws?appid=%s&authorization=%s&date=%s&host=%s&ts=%s",
                                  host, appId, authorization, dateStr, host, tsStr);
        return url;
    }

    /**
     * 模拟模式返回结果
     */
    private AsrResult getMockResult() {
        AsrResult result = new AsrResult();
        result.setText("（模拟识别）你好，这是一段测试语音的识别结果。");
        result.setSuccess(true);
        result.setMock(true);
        result.setConfidence(0.8);
        result.setDuration(2.5);
        return result;
    }

    /**
     * ASR结果封装
     */
    @lombok.Data
    public static class AsrResult {
        private String text;
        private boolean success;
        private boolean mock;
        private double confidence;
        private double duration;
    }
}
