package com.aiinterview.controller;

import com.aiinterview.common.Result;
import com.aiinterview.service.PositionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "岗位")
@RestController
@RequestMapping("/api/v1/positions")
@RequiredArgsConstructor
public class PositionController {

    private final PositionService positionService;

    @Operation(summary = "岗位列表")
    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        return Result.success(positionService.listActive());
    }

    @Operation(summary = "岗位详情")
    @GetMapping("/{code}")
    public Result<Map<String, Object>> detail(@PathVariable String code) {
        return Result.success(positionService.getByCode(code));
    }
}
