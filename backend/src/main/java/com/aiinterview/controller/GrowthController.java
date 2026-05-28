package com.aiinterview.controller;

import com.aiinterview.common.Result;
import com.aiinterview.service.GrowthService;
import com.aiinterview.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "成长曲线")
@RestController
@RequestMapping("/api/v1/growth")
@RequiredArgsConstructor
public class GrowthController {

    private final GrowthService growthService;

    @Operation(summary = "能力成长数据")
    @GetMapping
    public Result<Map<String, Object>> growth(
            @RequestParam(required = false) String positionCode,
            @RequestParam(defaultValue = "90") int days) {
        return Result.success(growthService.getGrowth(SecurityUtils.currentUserId(), positionCode, days));
    }
}
