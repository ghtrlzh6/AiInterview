package com.aiinterview.controller;

import com.aiinterview.common.Result;
import com.aiinterview.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "语音识别")
@RestController
@RequestMapping("/api/v1/asr")
public class AsrController {

    @Operation(summary = "语音转文字")
    @PostMapping("/convert")
    public Result<Map<String, Object>> convert(
            @RequestParam("audio") MultipartFile audio,
            @RequestParam(required = false) String sessionId) {
        SecurityUtils.currentUserId();
        Map<String, Object> data = new HashMap<>();
        data.put("text", "（模拟识别）Java 虚拟机内存模型主要分为堆、栈、方法区...");
        data.put("duration", 12.5);
        data.put("confidence", 0.95);
        return Result.success(data);
    }
}
