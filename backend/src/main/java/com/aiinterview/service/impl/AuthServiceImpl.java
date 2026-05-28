package com.aiinterview.service.impl;

import com.aiinterview.common.BusinessException;
import com.aiinterview.dto.auth.LoginRequest;
import com.aiinterview.dto.auth.RegisterRequest;
import com.aiinterview.entity.User;
import com.aiinterview.mapper.UserMapper;
import com.aiinterview.security.JwtUtil;
import com.aiinterview.service.AuthService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    @Override
    public Map<String, Object> register(RegisterRequest request) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname() : request.getUsername());
        user.setEmail(request.getEmail() != null ? request.getEmail() : "");
        user.setRole("USER");
        user.setTotalInterviews(0);
        userMapper.insert(user);
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        return data;
    }

    @Override
    public Map<String, Object> login(LoginRequest request) {
        User user = userMapper.findByUsername(request.getUsername());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw BusinessException.unauthorized("用户名或密码错误");
        }
        return buildTokenResponse(user);
    }

    @Override
    public Map<String, Object> refresh(String refreshToken) {
        try {
            Claims claims = jwtUtil.parseToken(refreshToken);
            if (!jwtUtil.isRefreshToken(claims)) {
                throw BusinessException.unauthorized("无效的 Refresh Token");
            }
            Long userId = Long.valueOf(claims.get("userId").toString());
            User user = userMapper.selectById(userId);
            if (user == null) {
                throw BusinessException.unauthorized("用户不存在");
            }
            Map<String, Object> data = new HashMap<>();
            data.put("accessToken", jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole()));
            data.put("expiresIn", jwtUtil.getExpireSeconds());
            return data;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw BusinessException.unauthorized("Token 已过期，请重新登录");
        }
    }

    @Override
    public void logout(String token) {
        if (StringUtils.hasText(token)) {
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "1", jwtUtil.getExpireSeconds(), TimeUnit.SECONDS);
        }
    }

    private Map<String, Object> buildTokenResponse(User user) {
        Map<String, Object> data = new HashMap<>();
        data.put("accessToken", jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole()));
        data.put("refreshToken", jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), user.getRole()));
        data.put("expiresIn", jwtUtil.getExpireSeconds());
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("avatarUrl", user.getAvatarUrl());
        userInfo.put("targetPositionCode", user.getTargetPositionCode());
        data.put("userInfo", userInfo);
        return data;
    }
}
