package com.aiinterview.service.ai;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.aiinterview.entity.Question;
import com.aiinterview.service.PromptService;
import com.aiinterview.util.PromptTemplateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 面试推进策略。
 * 负责模拟真人面试官：每轮先对候选人的回答给出一句自然反馈，
 * 再决定继续追问、进入下一题还是结束面试。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FollowUpStrategy {

    private static final int MAX_FOLLOW_UPS = 2;
    private static final Pattern ACTION_PATTERN =
            Pattern.compile("\"action\"\\s*:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern REPLY_PATTERN =
            Pattern.compile("\"(?:reply|content)\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final LlmService llmService;
    private final PromptService promptService;

    public enum Action {
        FOLLOW_UP, NEXT_QUESTION, END
    }

    /**
     * 面试官单轮决策。
     *
     * @param action 推进动作
     * @param reply  面试官要对候选人说的话（自然语言，已可直接展示）
     */
    public record Decision(Action action, String reply) {
    }

    public Decision decide(
            Question currentQuestion,
            String userAnswer,
            int followUpCount,
            int currentOrder,
            int totalQuestions,
            String positionName) {
        return decide(currentQuestion, userAnswer, followUpCount, currentOrder, totalQuestions,
                positionName, "", null, List.of());
    }

    public Decision decide(
            Question currentQuestion,
            String userAnswer,
            int followUpCount,
            int currentOrder,
            int totalQuestions,
            String positionName,
            String codingContext,
            String positionCode) {
        return decide(currentQuestion, userAnswer, followUpCount, currentOrder, totalQuestions,
                positionName, codingContext, positionCode, List.of());
    }

    public Decision decide(
            Question currentQuestion,
            String userAnswer,
            int followUpCount,
            int currentOrder,
            int totalQuestions,
            String positionName,
            String codingContext,
            String positionCode,
            List<LlmService.ChatMessage> history) {
        if (wantsToSkip(userAnswer)) {
            return transition(currentOrder, totalQuestions, "没问题，那我们先看下一题。");
        }
        if (followUpCount >= MAX_FOLLOW_UPS) {
            return transition(currentOrder, totalQuestions, "好的，这道题我们就先聊到这里。");
        }
        if (!StringUtils.hasText(userAnswer) || userAnswer.trim().length() < 15) {
            return new Decision(Action.FOLLOW_UP,
                    "这个回答有点简短，我想多了解一些。能不能结合具体的步骤、关键细节或你实际遇到过的例子再展开讲讲？");
        }

        String systemPrompt = buildSystemPrompt(positionCode, positionName, currentOrder, totalQuestions, currentQuestion);
        List<LlmService.ChatMessage> messages = new ArrayList<>();
        messages.add(new LlmService.ChatMessage("system", systemPrompt));
        if (history != null) {
            messages.addAll(history);
        }
        messages.add(new LlmService.ChatMessage("user",
                "【本题】" + currentQuestion.getTitle()
                        + "\n【候选人最新回答】" + userAnswer
                        + (StringUtils.hasText(codingContext) ? "\n【候选人提交的代码】\n" + codingContext : "")
                        + "\n【本题已追问次数】" + followUpCount + "（最多 " + MAX_FOLLOW_UPS + " 次）"
                        + "\n请严格按 system 中的 JSON 格式回复。"));
        String raw = llmService.chatJson(messages);
        return parseDecision(raw, currentOrder, totalQuestions);
    }

    private Decision parseDecision(String raw, int currentOrder, int totalQuestions) {
        if (!StringUtils.hasText(raw)) {
            log.warn("Interview LLM returned empty decision");
            return parseFallbackFollowUp();
        }
        try {
            return buildDecisionFromJson(JSONUtil.parseObj(extractJson(raw)), currentOrder, totalQuestions);
        } catch (Exception e) {
            log.warn("Interview decision JSON parse failed, raw={}", truncate(raw, 300), e);
            Decision recovered = tryRecoverFromText(raw, currentOrder, totalQuestions);
            if (recovered != null) {
                return recovered;
            }
            return parseFallbackFollowUp();
        }
    }

    private Decision buildDecisionFromJson(JSONObject json, int currentOrder, int totalQuestions) {
        String action = normalizeAction(json.getStr("action", "follow_up"));
        String reply = firstNonBlank(json.getStr("reply"), json.getStr("content"));
        return switch (action) {
            case "next_question", "next" -> transition(currentOrder, totalQuestions,
                    cleanReply(reply, "好的，我了解了，我们继续下一题。"));
            case "end", "finish" -> new Decision(Action.END,
                    cleanReply(reply, "好，今天的面试就到这里，感谢你的参与。"));
            default -> new Decision(Action.FOLLOW_UP,
                    cleanReply(reply, "能不能再具体说明一下你的思路和关键依据？"));
        };
    }

    private Decision tryRecoverFromText(String raw, int currentOrder, int totalQuestions) {
        Matcher actionMatcher = ACTION_PATTERN.matcher(raw);
        if (!actionMatcher.find()) {
            return null;
        }
        String action = normalizeAction(actionMatcher.group(1));
        String reply = null;
        Matcher replyMatcher = REPLY_PATTERN.matcher(raw);
        if (replyMatcher.find()) {
            reply = unescapeJsonString(replyMatcher.group(1));
        }
        try {
            return buildDecisionFromJson(new JSONObject()
                    .set("action", action)
                    .set("reply", reply != null ? reply : ""), currentOrder, totalQuestions);
        } catch (Exception e) {
            log.warn("Interview decision regex recovery failed", e);
            return null;
        }
    }

    private Decision parseFallbackFollowUp() {
        return new Decision(Action.FOLLOW_UP, "好的，我想再确认一下：你能结合具体例子，把刚才的思路和关键依据再讲清楚一些吗？");
    }

    private String normalizeAction(String action) {
        if (!StringUtils.hasText(action)) {
            return "follow_up";
        }
        return action.trim().toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private String unescapeJsonString(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private String truncate(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }

    private Decision transition(int currentOrder, int totalQuestions, String reply) {
        if (currentOrder >= totalQuestions) {
            return new Decision(Action.END,
                    StringUtils.hasText(reply) ? reply + "好，所有问题都聊完了，今天的面试到这里结束，感谢你的参与。"
                            : "好，所有问题都聊完了，今天的面试到这里结束，感谢你的参与。");
        }
        return new Decision(Action.NEXT_QUESTION, StringUtils.hasText(reply) ? reply : "好的，我们继续下一题。");
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

    private String cleanReply(String reply, String fallback) {
        if (!StringUtils.hasText(reply)) {
            return fallback;
        }
        String trimmed = reply.trim();
        if (trimmed.length() > 240) {
            trimmed = trimmed.substring(0, 240).trim();
        }
        return trimmed;
    }

    private String buildSystemPrompt(
            String positionCode, String positionName, int currentOrder, int totalQuestions, Question question) {
        String template = StringUtils.hasText(positionCode)
                ? promptService.getInterviewPrompt(positionCode)
                : promptService.getDefaultInterviewPrompt();
        String rendered = PromptTemplateUtil.render(template, Map.of(
                "positionName", positionName != null ? positionName : "",
                "totalQuestions", String.valueOf(totalQuestions),
                "currentOrder", String.valueOf(currentOrder),
                "questionTitle", question.getTitle() != null ? question.getTitle() : ""));
        return rendered + "\n\n【对话方式】你是一位真人技术面试官，语气自然、专业、克制，像在做一次平常的技术交流。"
                + "每一轮请先用一两句话对候选人的回答做出真实反馈：认可亮点，或委婉点出疏漏，但不要复述题目、不要给出标准答案、不要长篇大论。"
                + "然后判断：如果回答明显不完整、有错误或值得深入，就继续追问（同一题最多追问 " + MAX_FOLLOW_UPS + " 次）；"
                + "如果回答已经基本到位，就自然收尾进入下一题。注意：下一题由系统给出，你不要自己编造新题目，"
                + "进入下一题或结束时 reply 只写一句自然的过渡或收尾语即可。"
                + "\n【JSON 输出要求】你必须且只能输出一个合法 JSON 对象，不要 markdown 代码块，不要任何前后缀文字。"
                + "字段固定为 action 与 reply："
                + "\n{\"action\":\"follow_up|next_question|end\",\"reply\":\"面试官这一轮要对候选人说的话\"}";
    }

    private String extractJson(String raw) {
        String text = raw.trim();
        if (text.contains("```")) {
            int fenceStart = text.indexOf("```");
            int contentStart = text.indexOf('\n', fenceStart);
            if (contentStart > 0) {
                int fenceEnd = text.indexOf("```", contentStart + 1);
                if (fenceEnd > contentStart) {
                    text = text.substring(contentStart + 1, fenceEnd).trim();
                }
            }
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }
}
