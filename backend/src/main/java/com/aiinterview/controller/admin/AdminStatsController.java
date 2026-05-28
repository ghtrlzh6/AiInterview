package com.aiinterview.controller.admin;

import com.aiinterview.common.Result;
import com.aiinterview.service.admin.AdminStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "管理员-统计")
@RestController
@RequestMapping("/api/v1/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    @Operation(summary = "仪表盘统计")
    @GetMapping
    public Result<Map<String, Object>> stats() {
        return Result.success(adminStatsService.getStats());
    }
}
