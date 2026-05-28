package com.aiinterview.controller;

import com.aiinterview.common.Result;
import com.aiinterview.service.ResumeService;
import com.aiinterview.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "简历")
@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @Operation(summary = "上传简历")
    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        return Result.success(resumeService.upload(SecurityUtils.currentUserId(), file));
    }

    @Operation(summary = "解析状态")
    @GetMapping("/{resumeId}")
    public Result<Map<String, Object>> status(@PathVariable Long resumeId) {
        return Result.success(resumeService.getStatus(SecurityUtils.currentUserId(), resumeId));
    }

    @Operation(summary = "项目条目")
    @GetMapping("/{resumeId}/projects")
    public Result<List<Map<String, Object>>> projects(@PathVariable Long resumeId) {
        return Result.success(resumeService.listProjects(SecurityUtils.currentUserId(), resumeId));
    }
}
