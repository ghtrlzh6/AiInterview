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

    // @Override
    // public Map<String, Object> recommendations(Long userId, Long reportId) {
    //     EvaluationReport report = reportMapper.selectById(reportId);
    //     if (report == null || !report.getUserId().equals(userId)) {
    //         throw BusinessException.notFound("报告不存在");
    //     }
    //     // List<UserRecommendation> recs = recommendationMapper.selectList(new LambdaQueryWrapper<UserRecommendation>()
    //     //         .eq(UserRecommendation::getReportId, reportId)
    //     //         .eq(UserRecommendation::getUserId, userId));

    //     List<LearningResource> resources = new ArrayList<>();

    //     List<Map<String, Object>> list = recs.stream().map(rec -> {
    //         LearningResource r = resourceMapper.selectById(rec.getResourceId());
    //         Map<String, Object> m = new HashMap<>();
    //         m.put("recommendationId", rec.getId());
    //         m.put("reason", rec.getReason());
    //         if (r != null) {
    //             Map<String, Object> res = new HashMap<>();
    //             res.put("id", r.getId());
    //             res.put("title", r.getTitle());
    //             res.put("description", r.getDescription());
    //             res.put("resourceType", r.getResourceType());
    //             res.put("url", r.getUrl());
    //             res.put("topic", r.getTopic());
    //             res.put("difficulty", r.getDifficulty());
    //             m.put("resource", res);
    //         }
    //         return m;
    //     }).collect(Collectors.toList());
    //     Map<String, Object> result = new HashMap<>();
    //     result.put("reportId", reportId);
    //     result.put("recommendations", list);
    //     return result;
    // }

    @Override
    public Map<String, Object> recommendations(Long userId, Long reportId) {

        EvaluationReport report = reportMapper.selectById(reportId);

        if (report == null || !report.getUserId().equals(userId)) {
            throw BusinessException.notFound("报告不存在");
        }

        List<LearningResource> resources = new ArrayList<>();

        String positionCode = report.getPositionCode();

        // 技术能力不足
        if (report.getTechScore() != null
                && report.getTechScore().doubleValue() < 70) {

            resources.addAll(
                    resourceMapper.selectList(
                            new LambdaQueryWrapper<LearningResource>()
                                    .eq(LearningResource::getPositionCode, positionCode)
                                    .in(
                                            LearningResource::getTopic,
                                            Arrays.asList(
                                                    "Java基础",
                                                    "Spring",
                                                    "SpringBoot",
                                                    "MySQL",
                                                    "Python",
                                                    "Unity"
                                            )
                                    )
                    )
            );
        }

        // 逻辑能力不足
        if (report.getLogicScore() != null
                && report.getLogicScore().doubleValue() < 70) {

            resources.addAll(
                    resourceMapper.selectList(
                            new LambdaQueryWrapper<LearningResource>()
                                    .eq(LearningResource::getPositionCode, positionCode)
                                    .like(LearningResource::getTopic, "算法")
                    )
            );
        }

        // 深度能力不足
        if (report.getDepthScore() != null
                && report.getDepthScore().doubleValue() < 70) {

            resources.addAll(
                    resourceMapper.selectList(
                            new LambdaQueryWrapper<LearningResource>()
                                    .eq(LearningResource::getPositionCode, positionCode)
                                    .in(
                                            LearningResource::getTopic,
                                            Arrays.asList(
                                                    "JVM",
                                                    "并发编程",
                                                    "微服务",
                                                    "性能优化",
                                                    "Shader"
                                            )
                                    )
                    )
            );
        }

        // 如果啥都没命中，给岗位通用资源
        if (resources.isEmpty()) {

            resources = resourceMapper.selectList(
                    new LambdaQueryWrapper<LearningResource>()
                            .eq(LearningResource::getPositionCode, positionCode)
                            .last("limit 5")
            );
        }

        // 去重
        Map<Long, LearningResource> uniqueMap = new LinkedHashMap<>();

        for (LearningResource r : resources) {
            uniqueMap.put(r.getId(), r);
        }

        List<Map<String, Object>> list = new ArrayList<>();

        for (LearningResource r : uniqueMap.values()) {

            String reason = "根据本次面试结果推荐";

            if (report.getTechScore() != null
                    && report.getTechScore().doubleValue() < 70) {

                reason = "技术能力维度较弱，建议加强 " + r.getTopic();
            }

            if (report.getLogicScore() != null
                    && report.getLogicScore().doubleValue() < 70
                    && r.getTopic() != null
                    && r.getTopic().contains("算法")) {

                reason = "逻辑思维维度较弱，建议加强算法训练";
            }

            if (report.getDepthScore() != null
                    && report.getDepthScore().doubleValue() < 70
                    && Arrays.asList(
                            "JVM",
                            "并发编程",
                            "微服务",
                            "性能优化",
                            "Shader"
                    ).contains(r.getTopic())) {

                reason = "知识深度维度较弱，建议深入学习 " + r.getTopic();
            }

            Map<String, Object> item = new HashMap<>();

            item.put("recommendationId", r.getId());
            item.put("reason", reason);

            Map<String, Object> resource = new HashMap<>();

            resource.put("id", r.getId());
            resource.put("title", r.getTitle());
            resource.put("description", r.getDescription());
            resource.put("resourceType", r.getResourceType());
            resource.put("url", r.getUrl());
            resource.put("topic", r.getTopic());
            resource.put("difficulty", r.getDifficulty());

            item.put("resource", resource);

            list.add(item);
        }

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
