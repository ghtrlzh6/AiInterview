package com.aiinterview.service;

import com.aiinterview.common.PageResult;
import com.aiinterview.dto.user.UpdateProfileRequest;

import java.util.Map;

public interface UserService {

    Map<String, Object> getProfile(Long userId);

    Map<String, Object> updateProfile(Long userId, UpdateProfileRequest request);

    PageResult<Map<String, Object>> listInterviews(Long userId, int page, int size, String positionCode);
}
