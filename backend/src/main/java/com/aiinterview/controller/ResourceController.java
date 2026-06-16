package com.aiinterview.controller;

import com.aiinterview.common.PageResult;
import com.aiinterview.common.Result;
import com.aiinterview.service.ResourceService;
import com.aiinterview.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "学习资源")
@RestController
@RequestMapping("/api/v1/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @Operation(summary = "推荐资源")
    @GetMapping("/recommendations")
    public Result<Map<String, Object>> recommendations(@RequestParam Long reportId) {
        return Result.success(resourceService.recommendations(SecurityUtils.currentUserId(), reportId));
    }

    @Operation(summary = "资源反馈")
    @PostMapping("/recommendations/{recommendationId}/feedback")
    public Result<Void> feedback(@PathVariable Long recommendationId, @RequestBody FeedbackRequest request) {
        resourceService.feedback(SecurityUtils.currentUserId(), recommendationId, request.isHelpful());
        return Result.success();
    }

    @Operation(summary = "搜索资源")
    @GetMapping
    public Result<PageResult<Map<String, Object>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String positionCode,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(resourceService.search(keyword,positionCode, topic, type, page, size));
    }

    @Data
    public static class FeedbackRequest {
        private boolean isHelpful;
    }
}
