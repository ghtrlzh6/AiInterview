package com.aiinterview.service.impl;

import cn.hutool.core.util.IdUtil;
import com.aiinterview.common.BusinessException;
import com.aiinterview.common.PageResult;
import com.aiinterview.entity.DimensionScore;
import com.aiinterview.entity.EvaluationReport;
import com.aiinterview.entity.Position;
import com.aiinterview.entity.Question;
import com.aiinterview.mapper.*;
import com.aiinterview.service.ReportService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final EvaluationReportMapper reportMapper;
    private final DimensionScoreMapper dimensionScoreMapper;
    private final QuestionMapper questionMapper;
    private final PositionMapper positionMapper;

    @Value("${app.share-base-url:http://localhost/share}")
    private String shareBaseUrl;

    @Override
    public Map<String, Object> getReport(Long userId, Long reportId) {
        EvaluationReport report = requireOwned(userId, reportId);
        return toDetailMap(report, false);
    }

    @Override
    public PageResult<Map<String, Object>> listReports(Long userId, int page, int size, String positionCode) {
        LambdaQueryWrapper<EvaluationReport> wrapper = new LambdaQueryWrapper<EvaluationReport>()
                .eq(EvaluationReport::getUserId, userId)
                .eq(EvaluationReport::getReportStatus, "COMPLETED")
                .orderByDesc(EvaluationReport::getCreatedAt);
        if (StringUtils.hasText(positionCode)) {
            wrapper.eq(EvaluationReport::getPositionCode, positionCode);
        }
        Page<EvaluationReport> p = reportMapper.selectPage(new Page<>(page, size), wrapper);
        List<Map<String, Object>> list = p.getRecords().stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("reportId", r.getId());
            m.put("sessionId", r.getSessionId());
            m.put("positionCode", r.getPositionCode());
            m.put("overallScore", r.getOverallScore());
            m.put("reportStatus", r.getReportStatus());
            m.put("createdAt", r.getCreatedAt());
            return m;
        }).collect(Collectors.toList());
        return new PageResult<>(p.getTotal(), page, size, list);
    }

    @Override
    public Map<String, Object> share(Long userId, Long reportId) {
        EvaluationReport report = requireOwned(userId, reportId);
        if (!StringUtils.hasText(report.getShareToken())) {
            report.setShareToken(IdUtil.simpleUUID());
            reportMapper.updateById(report);
        }
        Map<String, Object> m = new HashMap<>();
        m.put("shareUrl", shareBaseUrl + "/" + report.getShareToken());
        m.put("shareToken", report.getShareToken());
        return m;
    }

    @Override
    public Map<String, Object> getByShareToken(String token) {
        EvaluationReport report = reportMapper.selectOne(new LambdaQueryWrapper<EvaluationReport>()
                .eq(EvaluationReport::getShareToken, token));
        if (report == null) {
            throw BusinessException.notFound("分享链接无效");
        }
        Map<String, Object> detail = toDetailMap(report, true);
        detail.remove("confidenceScore");
        return detail;
    }

    private Map<String, Object> toDetailMap(EvaluationReport report, boolean shared) {
        Map<String, Object> m = new HashMap<>();
        m.put("reportId", report.getId());
        m.put("sessionId", report.getSessionId());
        m.put("positionCode", report.getPositionCode());
        Position pos = positionMapper.selectOne(new LambdaQueryWrapper<Position>()
                .eq(Position::getCode, report.getPositionCode()));
        m.put("positionName", pos != null ? pos.getName() : report.getPositionCode());
        m.put("reportStatus", report.getReportStatus());
        m.put("overallScore", report.getOverallScore());
        Map<String, Object> scores = new HashMap<>();
        scores.put("tech", report.getTechScore());
        scores.put("expression", report.getExpressionScore());
        scores.put("logic", report.getLogicScore());
        scores.put("depth", report.getDepthScore());
        if (!shared) scores.put("confidence", report.getConfidenceScore());
        m.put("scores", scores);
        m.put("summary", report.getSummary());
        m.put("highlights", report.getHighlights());
        m.put("weaknesses", report.getWeaknesses());
        m.put("suggestions", report.getSuggestions());
        m.put("createdAt", report.getCreatedAt());
        List<DimensionScore> dsList = dimensionScoreMapper.selectList(new LambdaQueryWrapper<DimensionScore>()
                .eq(DimensionScore::getReportId, report.getId())
                .orderByAsc(DimensionScore::getQuestionOrder));
        List<Map<String, Object>> qScores = new ArrayList<>();
        for (DimensionScore ds : dsList) {
            Question q = questionMapper.selectById(ds.getQuestionId());
            Map<String, Object> qs = new HashMap<>();
            qs.put("questionOrder", ds.getQuestionOrder());
            qs.put("questionTitle", q != null ? q.getTitle() : "");
            qs.put("techScore", ds.getTechScore());
            qs.put("logicScore", ds.getLogicScore());
            qs.put("depthScore", ds.getDepthScore());
            qs.put("comment", ds.getComment());
            qScores.add(qs);
        }
        m.put("questionScores", qScores);
        return m;
    }

    private EvaluationReport requireOwned(Long userId, Long reportId) {
        EvaluationReport report = reportMapper.selectById(reportId);
        if (report == null || !report.getUserId().equals(userId)) {
            throw BusinessException.notFound("报告不存在");
        }
        return report;
    }
}
