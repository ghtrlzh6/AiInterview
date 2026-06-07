package com.aiinterview.controller;

import com.aiinterview.common.PageResult;
import com.aiinterview.common.Result;
import com.aiinterview.service.PromptService;
import com.aiinterview.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 题库管理控制器
 */
@Tag(name = "题库")
@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;
    private final PromptService promptService;

    @Operation(summary = "题目列表")
    @GetMapping
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam String positionCode,
            @RequestParam(required = false) String questionType,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) Long kbModuleId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(questionService.list(positionCode, questionType, difficulty, kbModuleId, page, size));
    }

    @Operation(summary = "获取岗位对应的面试官Prompt")
    @GetMapping("/prompt/{positionCode}")
    public Result<Map<String, Object>> getInterviewPrompt(@PathVariable String positionCode) {
        String prompt = promptService.getInterviewPrompt(positionCode);
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("positionCode", positionCode);
        data.put("prompt", prompt);
        return Result.success(data);
    }

    @Operation(summary = "获取岗位知识库节点列表")
    @GetMapping("/kb-nodes/{positionCode}")
    public Result<List<Map<String, Object>>> getKbNodesByPosition(@PathVariable String positionCode) {
        return Result.success(questionService.getKbNodesByPosition(positionCode));
    }

    @Operation(summary = "关联题目与知识库节点")
    @PostMapping("/{questionId}/kb-bind")
    public Result<Void> bindKbPoints(
            @PathVariable Long questionId,
            @RequestBody List<Long> kbNodeIds) {
        questionService.bindQuestionKbPoints(questionId, kbNodeIds);
        return Result.success(null);
    }

    @Operation(summary = "获取题目的知识库关联")
    @GetMapping("/{questionId}/kb-bind")
    public Result<List<Map<String, Object>>> getQuestionKbPoints(@PathVariable Long questionId) {
        return Result.success(questionService.getQuestionKbPoints(questionId));
    }

    @Operation(summary = "自动关联岗位题目与知识库（基于topic匹配）")
    @PostMapping("/auto-bind/{positionCode}")
    public Result<Map<String, Object>> autoBindQuestions(@PathVariable String positionCode) {
        int count = questionService.autoBindQuestionsByTopic(positionCode);
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("positionCode", positionCode);
        data.put("bindCount", count);
        return Result.success(data);
    }
}
