package com.aiinterview.controller;

import com.aiinterview.common.Result;
import com.aiinterview.security.SecurityUser;
import com.aiinterview.service.ai.XunfeiAsrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 语音识别控制器
 * 支持讯飞ASR和降级方案
 */
@Slf4j
@Tag(name = "语音识别")
@RestController
@RequestMapping("/api/v1/asr")
@RequiredArgsConstructor
public class AsrController {

    private final XunfeiAsrService xunfeiAsrService;

    private Long getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof SecurityUser user) {
                return user.getUserId();
            }
        } catch (Exception e) {
            log.debug("获取用户ID失败，可能未登录: {}", e.getMessage());
        }
        return null;
    }

    @Operation(summary = "语音转文字（文件上传方式）")
    @PostMapping("/convert")
    public Result<Map<String, Object>> convert(
            @RequestParam("audio") MultipartFile audio,
            @RequestParam(value = "format", defaultValue = "wav") String format,
            @RequestParam(value = "sampleRate", defaultValue = "16000") int sampleRate,
            @RequestParam(required = false) String sessionId) {
        Long userId = getCurrentUserId();
        log.info("用户 {} 请求语音识别（文件上传），会话ID: {}", userId, sessionId);

        Map<String, Object> data = new HashMap<>();

        try {
            byte[] audioData = audio.getBytes();
            XunfeiAsrService.AsrResult result = xunfeiAsrService.convertAudio(audioData, format, sampleRate);

            data.put("text", result.getText());
            data.put("duration", result.getDuration());
            data.put("confidence", result.getConfidence());
            data.put("isMock", result.isMock());

            return Result.success(data);
        } catch (Exception e) {
            log.error("语音识别失败", e);
            data.put("text", "未识别到语音");
            data.put("duration", 0);
            data.put("confidence", 0.0);
            data.put("isMock", true);
            data.put("error", e.getMessage());
            return Result.success(data);
        }
    }

    @Operation(summary = "语音转文字（Base64方式）")
    @PostMapping("/convert/base64")
    public Result<Map<String, Object>> convertBase64(@RequestBody AsrBase64Request request) {
        Long userId = getCurrentUserId();
        log.info("用户 {} 请求语音识别（Base64），会话ID: {}", userId, request.getSessionId());

        Map<String, Object> data = new HashMap<>();

        try {
            byte[] audioData = Base64.getDecoder().decode(request.getAudioBase64());
            String format = request.getFormat() != null ? request.getFormat() : "wav";
            int sampleRate = request.getSampleRate() != null ? request.getSampleRate() : 16000;

            XunfeiAsrService.AsrResult result = xunfeiAsrService.convertAudio(audioData, format, sampleRate);

            data.put("text", result.getText());
            data.put("duration", result.getDuration());
            data.put("confidence", result.getConfidence());
            data.put("isMock", result.isMock());

            return Result.success(data);
        } catch (Exception e) {
            log.error("语音识别失败", e);
            data.put("text", "未识别到语音");
            data.put("duration", 0);
            data.put("confidence", 0.0);
            data.put("isMock", true);
            data.put("error", e.getMessage());
            return Result.success(data);
        }
    }

    @Operation(summary = "检查语音识别能力")
    @GetMapping("/capability")
    public Result<Map<String, Object>> checkCapability() {
        Map<String, Object> data = new HashMap<>();
        data.put("supported", true);
        data.put("webSpeechAvailable", true);
        data.put("fallbackEnabled", true);
        return Result.success(data);
    }

    @Data
    public static class AsrBase64Request {
        private String audioBase64;
        private String format;
        private Integer sampleRate;
        private String sessionId;
    }
}
