package com.aiinterview.controller;

import com.aiinterview.common.PageResult;
import com.aiinterview.common.Result;
import com.aiinterview.dto.user.UpdateProfileRequest;
import com.aiinterview.service.UserService;
import com.aiinterview.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "用户")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "获取当前用户档案")
    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        return Result.success(userService.getProfile(SecurityUtils.currentUserId()));
    }

    @Operation(summary = "更新用户档案")
    @PutMapping("/me")
    public Result<Map<String, Object>> updateMe(@RequestBody UpdateProfileRequest request) {
        return Result.success(userService.updateProfile(SecurityUtils.currentUserId(), request));
    }

    @Operation(summary = "面试历史")
    @GetMapping("/me/interviews")
    public Result<PageResult<Map<String, Object>>> interviews(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String positionCode) {
        return Result.success(userService.listInterviews(SecurityUtils.currentUserId(), page, size, positionCode));
    }
}
