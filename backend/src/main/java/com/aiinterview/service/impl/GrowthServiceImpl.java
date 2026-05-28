package com.aiinterview.service.impl;

import com.aiinterview.entity.GrowthRecord;
import com.aiinterview.mapper.GrowthRecordMapper;
import com.aiinterview.service.GrowthService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GrowthServiceImpl implements GrowthService {

    private final GrowthRecordMapper growthRecordMapper;

    @Override
    public Map<String, Object> getGrowth(Long userId, String positionCode, int days) {
        LocalDate since = LocalDate.now().minusDays(days);
        LambdaQueryWrapper<GrowthRecord> wrapper = new LambdaQueryWrapper<GrowthRecord>()
                .eq(GrowthRecord::getUserId, userId)
                .ge(GrowthRecord::getRecordDate, since)
                .orderByAsc(GrowthRecord::getRecordDate);
        if (StringUtils.hasText(positionCode)) {
            wrapper.eq(GrowthRecord::getPositionCode, positionCode);
        }
        List<GrowthRecord> records = growthRecordMapper.selectList(wrapper);
        List<Map<String, Object>> list = records.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("recordDate", r.getRecordDate());
            m.put("overallScore", r.getOverallScore());
            m.put("techScore", r.getTechScore());
            m.put("expressionScore", r.getExpressionScore());
            m.put("logicScore", r.getLogicScore());
            m.put("depthScore", r.getDepthScore());
            m.put("confidenceScore", r.getConfidenceScore());
            m.put("sessionId", r.getSessionId());
            m.put("reportId", r.getReportId());
            return m;
        }).collect(Collectors.toList());
        Map<String, Object> trend = new HashMap<>();
        if (records.size() >= 2) {
            GrowthRecord first = records.get(0);
            GrowthRecord last = records.get(records.size() - 1);
            trend.put("overallChange", last.getOverallScore().subtract(first.getOverallScore()));
        } else {
            trend.put("overallChange", BigDecimal.ZERO);
        }
        trend.put("strongestDimension", "logic");
        trend.put("weakestDimension", "confidence");
        Map<String, Object> result = new HashMap<>();
        result.put("positionCode", positionCode);
        result.put("records", list);
        result.put("trend", trend);
        return result;
    }
}
