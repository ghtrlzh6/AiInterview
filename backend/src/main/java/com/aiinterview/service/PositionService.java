package com.aiinterview.service;

import com.aiinterview.entity.Position;

import java.util.List;
import java.util.Map;

public interface PositionService {

    List<Map<String, Object>> listActive();

    Map<String, Object> getByCode(String code);

    List<Position> listAllIncludingInactive();
}
