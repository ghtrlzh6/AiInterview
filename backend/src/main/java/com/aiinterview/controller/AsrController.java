package com.aiinterview.controller;

import com.aiinterview.common.Result;
import com.aiinterview.service.ai.XunfeiAsrService;
import com.aiinterview.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @Operation(summary = "语音转文字")
    @PostMapping("/convert")
    public Result<Map<String, Object>> convert(
            @RequestParam("audio") MultipartFile audio,
            @RequestParam(value = "format", defaultValue = "wav") String format,
            @RequestParam(value = "sampleRate", defaultValue = "16000") int sampleRate,
            @RequestParam(required = false) String sessionId) {
        Long userId = SecurityUtils.currentUserId();
        log.info("用户 {} 请求语音识别，会话ID: {}", userId, sessionId);

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
            // 降级返回模拟结果
            data.put("text", "（识别失败）请检查网络后重试...");
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
        data.put("webSpeechAvailable", true); // 前端应该检查Web Speech API
        data.put("fallbackEnabled", true);    // 启用了降级方案
        return Result.success(data);
    }
}
