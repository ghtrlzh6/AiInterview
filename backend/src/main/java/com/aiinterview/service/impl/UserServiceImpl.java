package com.aiinterview.service.impl;

import com.aiinterview.common.BusinessException;
import com.aiinterview.common.PageResult;
import com.aiinterview.dto.user.UpdateProfileRequest;
import com.aiinterview.entity.EvaluationReport;
import com.aiinterview.entity.InterviewSession;
import com.aiinterview.entity.Position;
import com.aiinterview.entity.User;
import com.aiinterview.mapper.EvaluationReportMapper;
import com.aiinterview.mapper.InterviewSessionMapper;
import com.aiinterview.mapper.PositionMapper;
import com.aiinterview.mapper.UserMapper;
import com.aiinterview.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PositionMapper positionMapper;
    private final InterviewSessionMapper sessionMapper;
    private final EvaluationReportMapper reportMapper;

    @Override
    public Map<String, Object> getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        return toProfileMap(user);
    }

    @Override
    public Map<String, Object> updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        if (StringUtils.hasText(request.getNickname())) user.setNickname(request.getNickname());
        if (request.getSchool() != null) user.setSchool(request.getSchool());
        if (request.getMajor() != null) user.setMajor(request.getMajor());
        if (request.getTargetPositionCode() != null) user.setTargetPositionCode(request.getTargetPositionCode());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        userMapper.updateById(user);
        return toProfileMap(user);
    }

    @Override
    public PageResult<Map<String, Object>> listInterviews(Long userId, int page, int size, String positionCode) {
        size = Math.min(Math.max(size, 1), 50);
        LambdaQueryWrapper<InterviewSession> wrapper = new LambdaQueryWrapper<InterviewSession>()
                .eq(InterviewSession::getUserId, userId)
                .orderByDesc(InterviewSession::getStartTime);
        if (StringUtils.hasText(positionCode)) {
            wrapper.eq(InterviewSession::getPositionCode, positionCode);
        }
        Page<InterviewSession> p = sessionMapper.selectPage(new Page<>(page, size), wrapper);
        List<Map<String, Object>> list = new ArrayList<>();
        for (InterviewSession s : p.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("sessionId", s.getId());
            item.put("positionCode", s.getPositionCode());
            Position pos = positionMapper.selectOne(new LambdaQueryWrapper<Position>().eq(Position::getCode, s.getPositionCode()));
            item.put("positionName", pos != null ? pos.getName() : s.getPositionCode());
            item.put("sessionStatus", s.getSessionStatus());
            item.put("durationSeconds", s.getDurationSeconds());
            item.put("startTime", s.getStartTime());
            item.put("endTime", s.getEndTime());
            EvaluationReport report = reportMapper.selectOne(new LambdaQueryWrapper<EvaluationReport>()
                    .eq(EvaluationReport::getSessionId, s.getId()));
            if (report != null) {
                item.put("overallScore", report.getOverallScore());
                item.put("reportId", report.getId());
            }
            list.add(item);
        }
        return new PageResult<>(p.getTotal(), page, size, list);
    }

    private Map<String, Object> toProfileMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", user.getId());
        map.put("username", user.getUsername());
        map.put("nickname", user.getNickname());
        map.put("avatarUrl", user.getAvatarUrl());
        map.put("email", user.getEmail());
        map.put("school", user.getSchool());
        map.put("major", user.getMajor());
        map.put("targetPositionCode", user.getTargetPositionCode());
        if (StringUtils.hasText(user.getTargetPositionCode())) {
            Position pos = positionMapper.selectOne(new LambdaQueryWrapper<Position>()
                    .eq(Position::getCode, user.getTargetPositionCode()));
            map.put("targetPositionName", pos != null ? pos.getName() : "");
        }
        map.put("totalInterviews", user.getTotalInterviews());
        map.put("createdAt", user.getCreatedAt());
        return map;
    }
}
