package com.aiinterview.service.ai;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.aiinterview.entity.Question;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

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
        if (followUpCount >= MAX_FOLLOW_UPS) {
            return nextOrEnd(currentOrder, totalQuestions, currentQuestion);
        }
        if (!StringUtils.hasText(userAnswer) || userAnswer.trim().length() < 20) {
            return new Decision(Action.FOLLOW_UP, "你的回答比较简短，能否展开说明一下关键细节？");
        }
        List<LlmService.ChatMessage> messages = List.of(
                new LlmService.ChatMessage("system",
                        "你是技术面试官。根据候选人回答决定 follow_up 或 next_question。"
                                + "输出 JSON：{\"action\":\"follow_up|next_question\",\"content\":\"...\"}"),
                new LlmService.ChatMessage("user",
                        "岗位：" + positionName + "\n题目：" + currentQuestion.getTitle()
                                + "\n回答：" + userAnswer + "\n已追问次数：" + followUpCount));
        String raw = llmService.chat(messages);
        return parseDecision(raw, currentOrder, totalQuestions, currentQuestion);
    }

    private Decision parseDecision(String raw, int currentOrder, int totalQuestions, Question question) {
        try {
            JSONObject json = JSONUtil.parseObj(extractJson(raw));
            String action = json.getStr("action", "follow_up");
            String content = json.getStr("content", "请继续补充你的回答。");
            return switch (action) {
                case "next_question" -> nextOrEnd(currentOrder, totalQuestions, question);
                case "end" -> new Decision(Action.END, content);
                default -> new Decision(Action.FOLLOW_UP, content);
            };
        } catch (Exception e) {
            return new Decision(Action.FOLLOW_UP, "能否再详细说明一下你的思路？");
        }
    }

    private Decision nextOrEnd(int currentOrder, int totalQuestions, Question question) {
        if (currentOrder >= totalQuestions) {
            return new Decision(Action.END, "感谢你的参与，本次面试到此结束。");
        }
        return new Decision(Action.NEXT_QUESTION, "**下一题：**" + question.getTitle());
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
