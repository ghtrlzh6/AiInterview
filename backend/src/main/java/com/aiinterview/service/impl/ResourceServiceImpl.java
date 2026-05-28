package com.aiinterview.service.impl;

import com.aiinterview.common.BusinessException;
import com.aiinterview.common.PageResult;
import com.aiinterview.entity.EvaluationReport;
import com.aiinterview.entity.LearningResource;
import com.aiinterview.entity.UserRecommendation;
import com.aiinterview.mapper.EvaluationReportMapper;
import com.aiinterview.mapper.LearningResourceMapper;
import com.aiinterview.mapper.UserRecommendationMapper;
import com.aiinterview.service.ResourceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

    private final UserRecommendationMapper recommendationMapper;
    private final LearningResourceMapper resourceMapper;
    private final EvaluationReportMapper reportMapper;

    @Override
    public Map<String, Object> recommendations(Long userId, Long reportId) {
        EvaluationReport report = reportMapper.selectById(reportId);
        if (report == null || !report.getUserId().equals(userId)) {
            throw BusinessException.notFound("报告不存在");
        }
        List<UserRecommendation> recs = recommendationMapper.selectList(new LambdaQueryWrapper<UserRecommendation>()
                .eq(UserRecommendation::getReportId, reportId)
                .eq(UserRecommendation::getUserId, userId));
        List<Map<String, Object>> list = recs.stream().map(rec -> {
            LearningResource r = resourceMapper.selectById(rec.getResourceId());
            Map<String, Object> m = new HashMap<>();
            m.put("recommendationId", rec.getId());
            m.put("reason", rec.getReason());
            if (r != null) {
                Map<String, Object> res = new HashMap<>();
                res.put("id", r.getId());
                res.put("title", r.getTitle());
                res.put("description", r.getDescription());
                res.put("resourceType", r.getResourceType());
                res.put("url", r.getUrl());
                res.put("topic", r.getTopic());
                res.put("difficulty", r.getDifficulty());
                m.put("resource", res);
            }
            return m;
        }).collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("reportId", reportId);
        result.put("recommendations", list);
        return result;
    }

    @Override
    public void feedback(Long userId, Long recommendationId, boolean isHelpful) {
        UserRecommendation rec = recommendationMapper.selectById(recommendationId);
        if (rec == null || !rec.getUserId().equals(userId)) {
            throw BusinessException.notFound("推荐记录不存在");
        }
        rec.setIsHelpful(isHelpful ? 1 : 0);
        recommendationMapper.updateById(rec);
    }

    @Override
    public PageResult<Map<String, Object>> search(String positionCode, String topic, String type, int page, int size) {
        LambdaQueryWrapper<LearningResource> wrapper = new LambdaQueryWrapper<LearningResource>()
                .orderByDesc(LearningResource::getQualityScore);
        if (StringUtils.hasText(positionCode)) {
            wrapper.and(w -> w.eq(LearningResource::getPositionCode, positionCode)
                    .or().eq(LearningResource::getPositionCode, ""));
        }
        if (StringUtils.hasText(topic)) {
            wrapper.like(LearningResource::getTopic, topic);
        }
        if (StringUtils.hasText(type)) {
            wrapper.eq(LearningResource::getResourceType, type);
        }
        Page<LearningResource> p = resourceMapper.selectPage(new Page<>(page, size), wrapper);
        List<Map<String, Object>> list = p.getRecords().stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("title", r.getTitle());
            m.put("description", r.getDescription());
            m.put("resourceType", r.getResourceType());
            m.put("url", r.getUrl());
            m.put("topic", r.getTopic());
            m.put("difficulty", r.getDifficulty());
            return m;
        }).collect(Collectors.toList());
        return new PageResult<>(p.getTotal(), page, size, list);
    }
}
