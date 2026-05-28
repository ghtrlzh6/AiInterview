package com.aiinterview.service.ai;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.aiinterview.entity.*;
import com.aiinterview.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiEvaluationService {

    private final LlmService llmService;
    private final EvaluationReportMapper reportMapper;
    private final DimensionScoreMapper dimensionScoreMapper;
    private final InterviewSessionMapper sessionMapper;
    private final InterviewQuestionMapper interviewQuestionMapper;
    private final QuestionMapper questionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final GrowthRecordMapper growthRecordMapper;
    private final UserMapper userMapper;
    private final LearningResourceMapper learningResourceMapper;
    private final UserRecommendationMapper recommendationMapper;

    @Async("taskExecutor")
    @Transactional
    public void evaluateAsync(Long reportId) {
        EvaluationReport report = reportMapper.selectById(reportId);
        if (report == null) {
            return;
        }
        try {
            InterviewSession session = sessionMapper.selectById(report.getSessionId());
            List<InterviewQuestion> iqs = interviewQuestionMapper.selectList(
                    new LambdaQueryWrapper<InterviewQuestion>()
                            .eq(InterviewQuestion::getSessionId, session.getId())
                            .orderByAsc(InterviewQuestion::getQuestionOrder));
            List<DimensionScore> scores = new ArrayList<>();
            Random random = new Random(reportId);
            BigDecimal techSum = BigDecimal.ZERO;
            BigDecimal logicSum = BigDecimal.ZERO;
            BigDecimal depthSum = BigDecimal.ZERO;
            for (InterviewQuestion iq : iqs) {
                Question q = questionMapper.selectById(iq.getQuestionId());
                String userAnswer = findUserAnswer(session.getId(), q.getId());
                DimensionScore ds = scoreQuestion(q, userAnswer, iq.getQuestionOrder(), reportId, session.getId(), random);
                dimensionScoreMapper.insert(ds);
                scores.add(ds);
                techSum = techSum.add(nullSafe(ds.getTechScore()));
                logicSum = logicSum.add(nullSafe(ds.getLogicScore()));
                depthSum = depthSum.add(nullSafe(ds.getDepthScore()));
            }
            int count = Math.max(iqs.size(), 1);
            BigDecimal techAvg = techSum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
            BigDecimal logicAvg = logicSum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
            BigDecimal depthAvg = depthSum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
            FinalReport finalReport = buildFinalReport(techAvg, logicAvg, depthAvg, scores);
            report.setTechScore(techAvg);
            report.setLogicScore(logicAvg);
            report.setDepthScore(depthAvg);
            report.setExpressionScore(finalReport.expressionScore());
            report.setConfidenceScore(finalReport.confidenceScore());
            report.setOverallScore(finalReport.overallScore());
            report.setSummary(finalReport.summary());
            report.setHighlights(finalReport.highlights());
            report.setWeaknesses(finalReport.weaknesses());
            report.setSuggestions(finalReport.suggestions());
            report.setReportStatus("COMPLETED");
            reportMapper.updateById(report);
            saveGrowthRecord(report, session);
            generateRecommendations(report);
            User user = userMapper.selectById(session.getUserId());
            if (user != null) {
                user.setTotalInterviews((user.getTotalInterviews() == null ? 0 : user.getTotalInterviews()) + 1);
                userMapper.updateById(user);
            }
        } catch (Exception e) {
            log.error("Evaluation failed for report {}", reportId, e);
            report.setReportStatus("FAILED");
            reportMapper.updateById(report);
        }
    }

    private DimensionScore scoreQuestion(
            Question q, String userAnswer, int order, Long reportId, Long sessionId, Random random) {
        DimensionScore ds = new DimensionScore();
        ds.setReportId(reportId);
        ds.setSessionId(sessionId);
        ds.setQuestionId(q.getId());
        ds.setQuestionOrder(order);
        if (llmService.isAvailable() && StringUtils.hasText(userAnswer)) {
            String prompt = "请评分并输出JSON：{\"tech_score\":0-100,\"logic_score\":0-100,\"depth_score\":0-100,\"comment\":\"...\"}\n"
                    + "题目：" + q.getTitle() + "\n回答：" + userAnswer;
            String raw = llmService.chat(List.of(new LlmService.ChatMessage("user", prompt)));
            try {
                JSONObject json = JSONUtil.parseObj(extractJson(raw));
                ds.setTechScore(toDecimal(json.getDouble("tech_score")));
                ds.setLogicScore(toDecimal(json.getDouble("logic_score")));
                ds.setDepthScore(toDecimal(json.getDouble("depth_score")));
                ds.setComment(json.getStr("comment", "表现良好"));
                return ds;
            } catch (Exception ignored) {
                // fall through to mock
            }
        }
        double base = 60 + random.nextInt(25);
        ds.setTechScore(BigDecimal.valueOf(base));
        ds.setLogicScore(BigDecimal.valueOf(base - 3 + random.nextInt(6)));
        ds.setDepthScore(BigDecimal.valueOf(base - 5 + random.nextInt(10)));
        ds.setComment("（模拟评分）对题目「" + truncate(q.getTitle(), 30) + "」的回答基本合格，建议继续深化相关知识点。");
        return ds;
    }

    private FinalReport buildFinalReport(BigDecimal tech, BigDecimal logic, BigDecimal depth, List<DimensionScore> scores) {
        if (llmService.isAvailable()) {
            String summaryText = scores.stream()
                    .map(s -> "Q" + s.getQuestionOrder() + ": tech=" + s.getTechScore())
                    .reduce((a, b) -> a + "; " + b).orElse("");
            String raw = llmService.chat(List.of(new LlmService.ChatMessage("user",
                    "汇总评分生成报告JSON：{\"overall_score\":0-100,\"expression_score\":0-100,\"confidence_score\":0-100,"
                            + "\"summary\":\"markdown\",\"highlights\":[],\"weaknesses\":[],\"suggestions\":[]}\n" + summaryText)));
            try {
                JSONObject json = JSONUtil.parseObj(extractJson(raw));
                return new FinalReport(
                        toDecimal(json.getDouble("overall_score")),
                        toDecimal(json.getDouble("expression_score")),
                        toDecimal(json.getDouble("confidence_score")),
                        json.getStr("summary", "## 综合评估\n\n本次面试已完成。"),
                        json.getBeanList("highlights", String.class),
                        json.getBeanList("weaknesses", String.class),
                        json.getBeanList("suggestions", String.class));
            } catch (Exception ignored) {
                // mock below
            }
        }
        BigDecimal overall = tech.add(logic).add(depth).divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
        return new FinalReport(
                overall,
                overall.subtract(BigDecimal.valueOf(2)),
                overall.subtract(BigDecimal.valueOf(5)),
                "## 综合评估\n\n（模拟报告）本次面试整体表现良好，继续保持练习节奏。",
                List.of("基础知识掌握较扎实", "表达清晰"),
                List.of("部分深度问题可继续加强", "系统设计思维有待提升"),
                List.of("建议针对性复习薄弱知识点", "多进行模拟面试练习"));
    }

    private void saveGrowthRecord(EvaluationReport report, InterviewSession session) {
        GrowthRecord gr = new GrowthRecord();
        gr.setUserId(report.getUserId());
        gr.setReportId(report.getId());
        gr.setSessionId(session.getId());
        gr.setPositionCode(report.getPositionCode());
        gr.setOverallScore(report.getOverallScore());
        gr.setTechScore(report.getTechScore());
        gr.setExpressionScore(report.getExpressionScore());
        gr.setLogicScore(report.getLogicScore());
        gr.setDepthScore(report.getDepthScore());
        gr.setConfidenceScore(report.getConfidenceScore());
        gr.setRecordDate(LocalDate.now());
        growthRecordMapper.insert(gr);
    }

    private void generateRecommendations(EvaluationReport report) {
        List<LearningResource> resources = learningResourceMapper.selectList(
                new LambdaQueryWrapper<LearningResource>()
                        .eq(LearningResource::getPositionCode, report.getPositionCode())
                        .last("LIMIT 3"));
        for (LearningResource r : resources) {
            UserRecommendation rec = new UserRecommendation();
            rec.setUserId(report.getUserId());
            rec.setReportId(report.getId());
            rec.setResourceId(r.getId());
            rec.setReason("基于本次面试表现推荐补充学习");
            rec.setIsClicked(0);
            recommendationMapper.insert(rec);
        }
    }

    private String findUserAnswer(Long sessionId, Long questionId) {
        List<ChatMessage> msgs = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .eq(ChatMessage::getQuestionId, questionId)
                        .eq(ChatMessage::getRole, "USER")
                        .orderByDesc(ChatMessage::getCreatedAt)
                        .last("LIMIT 1"));
        return msgs.isEmpty() ? "" : msgs.get(0).getContent();
    }

    private BigDecimal nullSafe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private BigDecimal toDecimal(Double v) {
        return v == null ? BigDecimal.ZERO : BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    private String extractJson(String raw) {
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return raw;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private record FinalReport(
            BigDecimal overallScore,
            BigDecimal expressionScore,
            BigDecimal confidenceScore,
            String summary,
            List<String> highlights,
            List<String> weaknesses,
            List<String> suggestions) {
    }
}
