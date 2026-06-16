package com.aiinterview.service;

import com.aiinterview.common.PageResult;

import java.util.Map;

public interface ResourceService {

    Map<String, Object> recommendations(Long userId, Long reportId);

    void feedback(Long userId, Long recommendationId, boolean isHelpful);

    PageResult<Map<String, Object>> search(String keyword ,String positionCode, String topic, String type, int page, int size);
}
