package com.aiinterview.service.admin.impl;

import com.aiinterview.entity.EvaluationReport;
import com.aiinterview.entity.InterviewSession;
import com.aiinterview.mapper.EvaluationReportMapper;
import com.aiinterview.mapper.InterviewSessionMapper;
import com.aiinterview.mapper.UserMapper;
import com.aiinterview.service.admin.AdminStatsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminStatsServiceImpl implements AdminStatsService {

    private final UserMapper userMapper;
    private final InterviewSessionMapper sessionMapper;
    private final EvaluationReportMapper reportMapper;

    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userMapper.selectCount(null));
        stats.put("totalInterviews", sessionMapper.selectCount(null));
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        stats.put("todayInterviews", sessionMapper.selectCount(new LambdaQueryWrapper<InterviewSession>()
                .ge(InterviewSession::getCreatedAt, startOfDay)));
        stats.put("completedReports", reportMapper.selectCount(new LambdaQueryWrapper<EvaluationReport>()
                .eq(EvaluationReport::getReportStatus, "COMPLETED")));
        List<InterviewSession> sessions = sessionMapper.selectList(null);
        Map<String, Long> byPosition = sessions.stream()
                .collect(Collectors.groupingBy(InterviewSession::getPositionCode, Collectors.counting()));
        List<Map<String, Object>> positionStats = byPosition.entrySet().stream().map(e -> {
            Map<String, Object> m = new HashMap<>();
            m.put("positionCode", e.getKey());
            m.put("count", e.getValue());
            return m;
        }).collect(Collectors.toList());
        stats.put("positionStats", positionStats);
        return stats;
    }
}
