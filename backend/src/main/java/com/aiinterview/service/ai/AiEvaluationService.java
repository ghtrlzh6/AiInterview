package com.aiinterview.service.ai;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.aiinterview.entity.*;
import com.aiinterview.mapper.*;
import com.aiinterview.service.PromptService;
import com.aiinterview.util.PromptTemplateUtil;
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
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiEvaluationService {

    private final LlmService llmService;
    private final RagService ragService;
    private final PromptService promptService;
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
            BigDecimal techSum = BigDecimal.ZERO;
            BigDecimal logicSum = BigDecimal.ZERO;
            BigDecimal depthSum = BigDecimal.ZERO;
            for (InterviewQuestion iq : iqs) {
                Question q = questionMapper.selectById(iq.getQuestionId());
                // 自我介绍属于开场暖身，不参与技术维度评分
                if (q == null || "SELF_INTRO".equals(q.getQuestionType())) {
                    continue;
                }
                String userAnswer = findUserAnswer(session.getId(), q.getId());
                DimensionScore ds = scoreQuestion(q, userAnswer, iq.getQuestionOrder(), reportId, session.getId(),
                        session.getPositionCode());
                dimensionScoreMapper.insert(ds);
                scores.add(ds);
                techSum = techSum.add(nullSafe(ds.getTechScore()));
                logicSum = logicSum.add(nullSafe(ds.getLogicScore()));
                depthSum = depthSum.add(nullSafe(ds.getDepthScore()));
            }
            int count = Math.max(scores.size(), 1);
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
            Question q, String userAnswer, int order, Long reportId, Long sessionId,
            String positionCode) {
        DimensionScore ds = new DimensionScore();
        ds.setReportId(reportId);
        ds.setSessionId(sessionId);
        ds.setQuestionId(q.getId());
        ds.setQuestionOrder(order);
        if (llmService.isAvailable() && StringUtils.hasText(userAnswer)) {
            String ragContext = ragService.buildContext(q.getTitle() + " " + userAnswer, positionCode, 3);
            String template = promptService.getEvaluationQuestionPrompt();
            String prompt = PromptTemplateUtil.render(template, Map.of(
                    "positionName", positionCode != null ? positionCode : "",
                    "questionTitle", q.getTitle() != null ? q.getTitle() : "",
                    "answerReference", q.getAnswerReference() != null ? q.getAnswerReference() : "",
                    "userAnswer", userAnswer));
            if (StringUtils.hasText(ragContext)) {
                prompt = ragContext + "\n\n" + prompt;
            }
            String raw = llmService.chat(List.of(new LlmService.ChatMessage("user", prompt)));
            try {
                JSONObject json = JSONUtil.parseObj(extractJson(raw));
                ds.setTechScore(toDecimal(json.getDouble("tech_score")));
                ds.setLogicScore(toDecimal(json.getDouble("logic_score")));
                ds.setDepthScore(toDecimal(json.getDouble("depth_score")));
                ds.setComment(json.getStr("comment", "表现良好"));
                return ds;
            } catch (Exception ignored) {
                // fall through to heuristic
            }
        }
        applyHeuristicScore(ds, q, userAnswer);
        return ds;
    }

    /**
     * 未配置 LLM 时的启发式评分：依据回答长度与对参考答案关键词的覆盖度估算，
     * 让"答得充分"明显高于"敷衍/不答"，使评分更符合逻辑。
     */
    private void applyHeuristicScore(DimensionScore ds, Question q, String userAnswer) {
        String answer = userAnswer == null ? "" : userAnswer.trim();
        if (answer.isEmpty()) {
            ds.setTechScore(BigDecimal.valueOf(40));
            ds.setLogicScore(BigDecimal.valueOf(42));
            ds.setDepthScore(BigDecimal.valueOf(38));
            ds.setComment("本题没有有效作答，建议复习相关知识点后再尝试。");
            return;
        }
        int len = answer.length();
        // 长度得分：约 200 字达到上限
        double lengthFactor = Math.min(len, 200) / 200.0;
        double coverage = keywordCoverage(q.getAnswerReference(), answer);

        double tech = clamp(48 + coverage * 38 + lengthFactor * 10);
        double logic = clamp(52 + lengthFactor * 22 + sentenceBonus(answer));
        double depth = clamp(46 + coverage * 26 + lengthFactor * 20);

        ds.setTechScore(round(tech));
        ds.setLogicScore(round(logic));
        ds.setDepthScore(round(depth));
        ds.setComment(buildHeuristicComment(q, len, coverage));
    }

    private double keywordCoverage(String reference, String answer) {
        if (!StringUtils.hasText(reference) || !StringUtils.hasText(answer)) {
            return 0.0;
        }
        String[] tokens = reference.split("[，,、;；/\\s。.（）()]+");
        int total = 0;
        int hit = 0;
        for (String token : tokens) {
            String key = token.trim();
            if (key.length() < 2) {
                continue;
            }
            total++;
            if (answer.contains(key)) {
                hit++;
            }
        }
        return total == 0 ? 0.0 : (double) hit / total;
    }

    private double sentenceBonus(String answer) {
        long sentences = answer.chars().filter(c -> c == '。' || c == '；' || c == '\n' || c == '.').count();
        return Math.min(sentences, 6);
    }

    private double clamp(double v) {
        return Math.max(30, Math.min(95, v));
    }

    private BigDecimal round(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    private String buildHeuristicComment(Question q, int len, double coverage) {
        String title = truncate(q.getTitle(), 30);
        if (coverage >= 0.5) {
            return "对「" + title + "」的回答覆盖了多数关键点，思路较完整，可继续补充细节与边界情况。";
        }
        if (len >= 80) {
            return "对「" + title + "」的回答展开较充分，但部分关键要点尚未点到，建议对照知识点查漏补缺。";
        }
        return "对「" + title + "」的回答较为简略，建议结合原理与实例进一步展开。";
    }

    private FinalReport buildFinalReport(BigDecimal tech, BigDecimal logic, BigDecimal depth, List<DimensionScore> scores) {
        if (llmService.isAvailable()) {
            String summaryText = scores.stream()
                    .map(s -> "Q" + s.getQuestionOrder() + ": tech=" + s.getTechScore()
                            + ", logic=" + s.getLogicScore() + ", depth=" + s.getDepthScore())
                    .reduce((a, b) -> a + "; " + b).orElse("");
            String template = promptService.getEvaluationFinalPrompt();
            String prompt = PromptTemplateUtil.render(template, Map.of("scoreSummary", summaryText));
            String raw = llmService.chat(List.of(new LlmService.ChatMessage("user", prompt)));
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
        BigDecimal expression = clampScore(logic.add(BigDecimal.valueOf(2)));
        BigDecimal confidence = clampScore(overall.subtract(BigDecimal.valueOf(4)));

        Map<String, BigDecimal> dims = new LinkedHashMap<>();
        dims.put("技术准确性", tech);
        dims.put("逻辑表达", logic);
        dims.put("回答深度", depth);
        String strongest = dims.entrySet().stream().max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("综合能力");
        String weakest = dims.entrySet().stream().min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("综合能力");

        String level = overall.doubleValue() >= 80 ? "整体表现优秀"
                : overall.doubleValue() >= 65 ? "整体表现良好" : "整体表现一般，仍有较大提升空间";
        String summary = "## 综合评估\n\n本次面试" + level + "，综合得分约 " + overall + " 分。"
                + "你在「" + strongest + "」方面表现相对突出，而「" + weakest + "」是当前最值得加强的方向。"
                + "建议结合下方的改进建议，针对薄弱维度做专项练习。";

        return new FinalReport(
                overall,
                expression,
                confidence,
                summary,
                List.of(strongest + "表现较好", "作答态度认真，能围绕问题展开"),
                List.of(weakest + "有待加强", "部分回答的深度和细节还可以更充分"),
                List.of("针对「" + weakest + "」做专项复习与练习", "多用 STAR 法则组织项目类回答", "保持规律的模拟面试节奏"));
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
        String weakTopic = resolveWeakTopic(report);
        LambdaQueryWrapper<LearningResource> wrapper = new LambdaQueryWrapper<LearningResource>()
                .eq(LearningResource::getPositionCode, report.getPositionCode())
                .orderByDesc(LearningResource::getQualityScore);
        if (StringUtils.hasText(weakTopic)) {
            wrapper.like(LearningResource::getTopic, weakTopic);
        }
        List<LearningResource> resources = learningResourceMapper.selectList(wrapper.last("LIMIT 5"));
        if (resources.isEmpty()) {
            resources = learningResourceMapper.selectList(new LambdaQueryWrapper<LearningResource>()
                    .eq(LearningResource::getPositionCode, report.getPositionCode())
                    .last("LIMIT 3"));
        }
        for (LearningResource r : resources) {
            UserRecommendation rec = new UserRecommendation();
            rec.setUserId(report.getUserId());
            rec.setReportId(report.getId());
            rec.setResourceId(r.getId());
            rec.setReason(StringUtils.hasText(weakTopic)
                    ? "针对薄弱维度「" + weakTopic + "」推荐补充学习"
                    : "基于本次面试表现推荐补充学习");
            rec.setIsClicked(0);
            recommendationMapper.insert(rec);
        }
    }

    private String resolveWeakTopic(EvaluationReport report) {
        if (report.getWeaknesses() != null && !report.getWeaknesses().isEmpty()) {
            return report.getWeaknesses().get(0);
        }
        Map<String, BigDecimal> dims = new LinkedHashMap<>();
        dims.put("技术深度", nullSafe(report.getTechScore()));
        dims.put("逻辑表达", nullSafe(report.getLogicScore()));
        dims.put("回答深度", nullSafe(report.getDepthScore()));
        return dims.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("综合能力");
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

    private BigDecimal clampScore(BigDecimal v) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        if (v.doubleValue() < 0) {
            return BigDecimal.ZERO;
        }
        if (v.doubleValue() > 100) {
            return BigDecimal.valueOf(100);
        }
        return v;
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
