package com.aiinterview.service.ai;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.aiinterview.entity.Question;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class FollowUpStrategy {

    private static final int MAX_FOLLOW_UPS = 2;

    private final LlmService llmService;

    public enum Action {
        FOLLOW_UP, NEXT_QUESTION, END
    }

    public record Decision(Action action, String content) {
    }

    public Decision decide(
            Question currentQuestion,
            String userAnswer,
            int followUpCount,
            int currentOrder,
            int totalQuestions,
            String positionName) {
        return decide(currentQuestion, userAnswer, followUpCount, currentOrder, totalQuestions, positionName, "");
    }

    public Decision decide(
            Question currentQuestion,
            String userAnswer,
            int followUpCount,
            int currentOrder,
            int totalQuestions,
            String positionName,
            String codingContext) {
        if (wantsToSkip(userAnswer)) {
            return nextOrEnd(currentOrder, totalQuestions, currentQuestion);
        }
        if (followUpCount >= MAX_FOLLOW_UPS) {
            return nextOrEnd(currentOrder, totalQuestions, currentQuestion);
        }
        if (!StringUtils.hasText(userAnswer) || userAnswer.trim().length() < 20) {
            return new Decision(Action.FOLLOW_UP, "你的回答比较简短，能否结合关键步骤、边界条件或实际经验再展开说明一下？");
        }
        List<LlmService.ChatMessage> messages = List.of(
                new LlmService.ChatMessage("system",
                        "你是技术面试官，只负责推进面试节奏。根据候选人回答决定 follow_up 或 next_question。"
                                + "禁止讲解标准答案，禁止大段点评，禁止替候选人补充答案。"
                                + "如果需要 follow_up，content 必须是一个简短追问，聚焦候选人回答中缺失、含糊或可深挖的一点。"
                                + "如果回答已经基本可判断，action 必须是 next_question，content 留空字符串。"
                                + "只输出 JSON：{\"action\":\"follow_up|next_question\",\"content\":\"...\"}"),
                new LlmService.ChatMessage("user",
                        "岗位：" + positionName + "\n题目：" + currentQuestion.getTitle()
                                + "\n回答：" + userAnswer
                                + (StringUtils.hasText(codingContext) ? "\n代码提交上下文：\n" + codingContext : "")
                                + "\n已追问次数：" + followUpCount));
        String raw = llmService.chat(messages);
        return parseDecision(raw, currentOrder, totalQuestions, currentQuestion);
    }

    private Decision parseDecision(String raw, int currentOrder, int totalQuestions, Question question) {
        try {
            JSONObject json = JSONUtil.parseObj(extractJson(raw));
            String action = json.getStr("action", "follow_up");
            String content = json.getStr("content", "能否再具体说明一下你的思路和关键依据？");
            return switch (action) {
                case "next_question" -> nextOrEnd(currentOrder, totalQuestions, question);
                case "end" -> new Decision(Action.END, content);
                default -> new Decision(Action.FOLLOW_UP, sanitizeFollowUp(content));
            };
        } catch (Exception e) {
            return new Decision(Action.FOLLOW_UP, "能否再具体说明一下你的思路和关键依据？");
        }
    }

    private Decision nextOrEnd(int currentOrder, int totalQuestions, Question question) {
        if (currentOrder >= totalQuestions) {
            return new Decision(Action.END, "感谢你的参与，本次面试到此结束。");
        }
        return new Decision(Action.NEXT_QUESTION, "");
    }

    private boolean wantsToSkip(String userAnswer) {
        if (!StringUtils.hasText(userAnswer)) {
            return false;
        }
        String normalized = userAnswer.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        return normalized.contains("\u4e0b\u4e00\u9898")
                || normalized.contains("\u8df3\u8fc7")
                || normalized.contains("\u6362\u4e00\u9898")
                || normalized.contains("\u8fd9\u9898\u4e0d\u4f1a")
                || normalized.contains("\u8fd9\u4e2a\u95ee\u9898\u4e0d\u4f1a")
                || normalized.contains("nextquestion")
                || normalized.contains("skipthis");
    }

    private String sanitizeFollowUp(String content) {
        if (!StringUtils.hasText(content)) {
            return "能否再具体说明一下你的思路和关键依据？";
        }
        String trimmed = content.trim();
        if (trimmed.length() > 120) {
            trimmed = trimmed.substring(0, 120);
        }
        if (!trimmed.endsWith("？") && !trimmed.endsWith("?")) {
            trimmed = trimmed + "？";
        }
        return trimmed;
    }

    private String extractJson(String raw) {
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return raw;
    }
}
