package com.aiinterview.service.ai;

import com.aiinterview.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 讯飞语音识别服务
 * 支持实时语音转文字和音频文件转写
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

    @Value("${ai.xunfei.asr-url:}")
    private String asrUrl;

    @Value("${ai.xunfei.enabled:false}")
    private boolean enabled;

    /**
     * 音频文件转文字
     * @param audioData 音频数据
     * @param format 音频格式 (pcm, wav, mp3)
     * @param sampleRate 采样率 (16000, 8000)
     * @return 识别结果
     */
    public AsrResult convertAudio(byte[] audioData, String format, int sampleRate) {
        if (!enabled || apiKey == null || apiKey.isEmpty()) {
            log.warn("讯飞ASR未启用或未配置API Key，使用模拟模式");
            return getMockResult();
        }

        try {
            return callXunfeiApi(audioData, format, sampleRate);
        } catch (Exception e) {
            log.error("讯飞ASR调用失败: {}", e.getMessage());
            throw new BusinessException("语音识别失败: " + e.getMessage());
        }
    }

    /**
     * 调用讯飞API
     */
    private AsrResult callXunfeiApi(byte[] audioData, String format, int sampleRate) throws Exception {
        // 构建请求URL
        String hostUrl = asrUrl + "?appid=" + appId;
        URL url = new URL(hostUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // 设置请求头
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "audio/" + format);
        conn.setRequestProperty("Authorization", buildAuth());
        conn.setDoOutput(true);

        // 发送音频数据
        try (OutputStream os = conn.getOutputStream()) {
            os.write(audioData);
        }

        // 读取响应
        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return parseResponse(response.toString());
            }
        } else {
            throw new BusinessException("讯飞API调用失败，响应码: " + responseCode);
        }
    }

    /**
     * 构建鉴权header
     */
    private String buildAuth() {
        // 讯飞WebSocket鉴权算法
        String ts = String.valueOf(System.currentTimeMillis() / 1000);
        String signStr = apiKey + ts;
        String sign = "";
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(signStr.getBytes(StandardCharsets.UTF_8));
            sign = Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("签名计算失败", e);
        }
        return "sign=" + sign + ", ts=" + ts;
    }

    /**
     * 解析讯飞API响应
     */
    private AsrResult parseResponse(String response) {
        AsrResult result = new AsrResult();
        // 实际解析时需要根据讯飞API文档解析JSON响应
        // 这里简化处理
        result.setText(response);
        result.setSuccess(true);
        return result;
    }

    /**
     * 模拟模式返回结果
     */
    private AsrResult getMockResult() {
        AsrResult result = new AsrResult();
        result.setText("（模拟识别）这是一段测试语音的识别结果...");
        result.setSuccess(true);
        result.setMock(true);
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
