package com.aiinterview.controller;

import com.aiinterview.common.Result;
import com.aiinterview.dto.interview.CodingSubmitRequest;
import com.aiinterview.dto.interview.EndInterviewRequest;
import com.aiinterview.dto.interview.SendMessageRequest;
import com.aiinterview.dto.interview.StartInterviewRequest;
import com.aiinterview.service.InterviewService;
import com.aiinterview.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@Tag(name = "面试")
@RestController
@RequestMapping("/api/v1/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @Operation(summary = "当前进行中的面试")
    @GetMapping("/active")
    public Result<Map<String, Object>> active() {
        return Result.success(interviewService.getActiveSession(SecurityUtils.currentUserId()));
    }

    @Operation(summary = "开始面试")
    @PostMapping("/start")
    public Result<Map<String, Object>> start(@Valid @RequestBody StartInterviewRequest request) {
        return Result.success(interviewService.start(SecurityUtils.currentUserId(), request));
    }

    @Operation(summary = "发送消息(SSE)")
    @PostMapping(value = "/{sessionId}/message", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter message(@PathVariable Long sessionId, @RequestBody SendMessageRequest request) {
        return interviewService.sendMessage(SecurityUtils.currentUserId(), sessionId, request);
    }

    @Operation(summary = "结束面试")
    @PostMapping("/{sessionId}/end")
    public Result<Map<String, Object>> end(
            @PathVariable Long sessionId,
            @RequestBody(required = false) EndInterviewRequest request) {
        return Result.success(interviewService.end(
                SecurityUtils.currentUserId(),
                sessionId,
                request != null ? request : new EndInterviewRequest()));
    }

    @Operation(summary = "生成面试报告")
    @PostMapping("/{sessionId}/report")
    public Result<Map<String, Object>> generateReport(@PathVariable Long sessionId) {
        return Result.success(interviewService.generateReport(SecurityUtils.currentUserId(), sessionId));
    }

    @Operation(summary = "会话详情")
    @GetMapping("/{sessionId}")
    public Result<Map<String, Object>> detail(@PathVariable Long sessionId) {
        return Result.success(interviewService.getSession(SecurityUtils.currentUserId(), sessionId));
    }

    @Operation(summary = "对话记录")
    @GetMapping("/{sessionId}/messages")
    public Result<Map<String, Object>> messages(@PathVariable Long sessionId) {
        return Result.success(interviewService.getMessages(SecurityUtils.currentUserId(), sessionId));
    }

    @Operation(summary = "手撕代码提交")
    @PostMapping("/{sessionId}/coding-submit")
    public Result<Map<String, Object>> codingSubmit(
            @PathVariable Long sessionId,
            @Valid @RequestBody CodingSubmitRequest request) {
        return Result.success(interviewService.codingSubmit(SecurityUtils.currentUserId(), sessionId, request));
    }

    @Operation(summary = "最近一次手撕代码提交")
    @GetMapping("/{sessionId}/coding-submit/latest")
    public Result<Map<String, Object>> latestCodingSubmit(
            @PathVariable Long sessionId,
            @RequestParam Long questionId) {
        return Result.success(interviewService.latestCodingSubmit(SecurityUtils.currentUserId(), sessionId, questionId));
    }
}
