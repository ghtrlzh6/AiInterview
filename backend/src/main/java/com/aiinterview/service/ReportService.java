package com.aiinterview.service;

import com.aiinterview.common.PageResult;

import java.util.Map;

public interface ReportService {

    Map<String, Object> getReport(Long userId, Long reportId);

    PageResult<Map<String, Object>> listReports(Long userId, int page, int size, String positionCode);

    Map<String, Object> share(Long userId, Long reportId);

    Map<String, Object> getByShareToken(String token);
}
