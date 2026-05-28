package com.aiinterview.controller;

import com.aiinterview.common.Result;
import com.aiinterview.dto.auth.LoginRequest;
import com.aiinterview.dto.auth.RefreshTokenRequest;
import com.aiinterview.dto.auth.RegisterRequest;
import com.aiinterview.service.AuthService;
import com.aiinterview.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "认证")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @Operation(summary = "刷新 Token")
    @PostMapping("/refresh")
    public Result<Map<String, Object>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return Result.success(authService.refresh(request.getRefreshToken()));
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            authService.logout(header.substring(7));
        }
        SecurityUtils.currentUser();
        return Result.success();
    }
}
