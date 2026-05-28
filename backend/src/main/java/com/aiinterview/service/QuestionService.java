package com.aiinterview.service;

import com.aiinterview.common.PageResult;

import java.util.Map;

public interface QuestionService {

    PageResult<Map<String, Object>> list(String positionCode, String questionType, Integer difficulty,
                                         Long kbModuleId, int page, int size);
}
