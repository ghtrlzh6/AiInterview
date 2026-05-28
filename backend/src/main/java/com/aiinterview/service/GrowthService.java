package com.aiinterview.service;

import java.util.Map;

public interface GrowthService {

    Map<String, Object> getGrowth(Long userId, String positionCode, int days);
}
