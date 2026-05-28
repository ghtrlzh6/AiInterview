package com.aiinterview.controller;

import com.aiinterview.common.PageResult;
import com.aiinterview.common.Result;
import com.aiinterview.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "题库")
@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

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
}
