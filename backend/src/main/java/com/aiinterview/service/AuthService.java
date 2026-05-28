package com.aiinterview.service;

import com.aiinterview.dto.auth.LoginRequest;
import com.aiinterview.dto.auth.RegisterRequest;

import java.util.Map;

public interface AuthService {

    Map<String, Object> register(RegisterRequest request);

    Map<String, Object> login(LoginRequest request);

    Map<String, Object> refresh(String refreshToken);

    void logout(String token);
}
