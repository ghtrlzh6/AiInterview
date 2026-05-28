package com.aiinterview.service.impl;

import cn.hutool.json.JSONUtil;
import com.aiinterview.common.BusinessException;
import com.aiinterview.dto.interview.CodingSubmitRequest;
import com.aiinterview.dto.interview.SendMessageRequest;
import com.aiinterview.dto.interview.StartInterviewRequest;
import com.aiinterview.entity.*;
import com.aiinterview.mapper.*;
import com.aiinterview.service.InterviewService;
import com.aiinterview.service.ai.AiEvaluationService;
import com.aiinterview.service.ai.FollowUpStrategy;
import com.aiinterview.service.ai.LlmService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    private final InterviewSessionMapper sessionMapper;
    private final InterviewQuestionMapper interviewQuestionMapper;
    private final QuestionMapper questionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final PositionMapper positionMapper;
    private final UserResumeMapper resumeMapper;
    private final SessionCodingSubmitMapper codingSubmitMapper;
    private final EvaluationReportMapper reportMapper;
    private final FollowUpStrategy followUpStrategy;
    private final LlmService llmService;
    private final AiEvaluationService aiEvaluationService;

    private final Map<Long, SessionContext> sessionContexts = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public Map<String, Object> start(Long userId, StartInterviewRequest request) {
        Position position = positionMapper.selectOne(new LambdaQueryWrapper<Position>()
                .eq(Position::getCode, request.getPositionCode()));
        if (position == null) {
            throw new BusinessException("岗位不存在");
        }
        if (request.getResumeSnapshotId() != null) {
            UserResume resume = resumeMapper.selectById(request.getResumeSnapshotId());
            if (resume == null || !resume.getUserId().equals(userId)) {
                throw new BusinessException("简历不存在");
            }
            if (!"SUCCESS".equals(resume.getParseStatus())) {
                throw new BusinessException("简历尚未解析完成");
            }
        }
        int count = request.getQuestionCount() != null ? request.getQuestionCount() : 8;
        List<Question> questions = pickQuestions(request.getPositionCode(), count);
        if (questions.isEmpty()) {
            questions = createFallbackQuestions(request.getPositionCode(), count);
        }
        InterviewSession session = new InterviewSession();
        session.setUserId(userId);
        session.setResumeSnapshotId(request.getResumeSnapshotId());
        session.setPositionCode(request.getPositionCode());
        session.setSessionStatus("IN_PROGRESS");
        session.setInputMode(StringUtils.hasText(request.getInputMode()) ? request.getInputMode() : "TEXT");
        session.setTotalQuestions(questions.size());
        session.setAnsweredCount(0);
        session.setStartTime(LocalDateTime.now());
        sessionMapper.insert(session);
        int order = 1;
        for (Question q : questions) {
            InterviewQuestion iq = new InterviewQuestion();
            iq.setSessionId(session.getId());
            iq.setQuestionId(q.getId());
            iq.setQuestionOrder(order++);
            iq.setIsAnswered(0);
            interviewQuestionMapper.insert(iq);
        }
        Question firstQ = questions.get(0);
        String greeting = buildGreeting(position.getName(), questions.size(), firstQ.getTitle());
        ChatMessage firstMsg = saveMessage(session.getId(), firstQ.getId(), "ASSISTANT", greeting, "QUESTION", 1);
        SessionContext ctx = new SessionContext();
        ctx.currentOrder = 1;
        ctx.followUpCount = 0;
        sessionContexts.put(session.getId(), ctx);
        Map<String, Object> data = new HashMap<>();
        data.put("sessionId", session.getId());
        data.put("positionCode", session.getPositionCode());
        data.put("positionName", position.getName());
        data.put("totalQuestions", session.getTotalQuestions());
        Map<String, Object> msg = new HashMap<>();
        msg.put("messageId", firstMsg.getId());
        msg.put("role", "ASSISTANT");
        msg.put("content", firstMsg.getContent());
        msg.put("messageType", "QUESTION");
        msg.put("questionOrder", 1);
        data.put("firstMessage", msg);
        return data;
    }

    @Override
    public SseEmitter sendMessage(Long userId, Long sessionId, SendMessageRequest request) {
        InterviewSession session = requireSession(userId, sessionId);
        if (!"IN_PROGRESS".equals(session.getSessionStatus())) {
            throw new BusinessException("面试已结束");
        }
        SessionContext ctx = sessionContexts.computeIfAbsent(sessionId, k -> new SessionContext());
        InterviewQuestion currentIq = getCurrentQuestion(sessionId, ctx.currentOrder);
        Question currentQ = questionMapper.selectById(currentIq.getQuestionId());
        saveMessage(sessionId, currentQ.getId(), "USER", request.getContent(), request.getMessageType(), ctx.currentOrder);
        Position position = positionMapper.selectOne(new LambdaQueryWrapper<Position>()
                .eq(Position::getCode, session.getPositionCode()));
        FollowUpStrategy.Decision decision = followUpStrategy.decide(
                currentQ, request.getContent(), ctx.followUpCount,
                ctx.currentOrder, session.getTotalQuestions(),
                position != null ? position.getName() : session.getPositionCode());
        SseEmitter emitter = new SseEmitter(120000L);
        new Thread(() -> {
            try {
                streamResponse(emitter, decision, session, ctx, currentQ, currentIq);
            } catch (Exception e) {
                log.error("SSE error", e);
                sendEvent(emitter, Map.of("type", "error", "content", e.getMessage()));
                emitter.completeWithError(e);
            }
        }).start();
        return emitter;
    }

    private void streamResponse(SseEmitter emitter, FollowUpStrategy.Decision decision,
                                InterviewSession session, SessionContext ctx,
                                Question currentQ, InterviewQuestion currentIq) throws IOException {
        String content = decision.content();
        llmService.chatStream(
                List.of(new LlmService.ChatMessage("user", "请用自然语言回复：" + content)),
                token -> sendEvent(emitter, Map.of("type", "token", "content", token)));
        String messageType;
        Long reportId = null;
        switch (decision.action()) {
            case FOLLOW_UP -> {
                ctx.followUpCount++;
                messageType = "FOLLOW_UP";
            }
            case NEXT_QUESTION -> {
                currentIq.setIsAnswered(1);
                interviewQuestionMapper.updateById(currentIq);
                session.setAnsweredCount(session.getAnsweredCount() + 1);
                sessionMapper.updateById(session);
                ctx.followUpCount = 0;
                ctx.currentOrder++;
                messageType = "QUESTION";
                InterviewQuestion nextIq = getCurrentQuestion(session.getId(), ctx.currentOrder);
                if (nextIq != null) {
                    Question nextQ = questionMapper.selectById(nextIq.getQuestionId());
                    content = "**下一题：**" + nextQ.getTitle();
                    sendEvent(emitter, Map.of("type", "next_question", "content", content));
                }
            }
            case END -> {
                currentIq.setIsAnswered(1);
                interviewQuestionMapper.updateById(currentIq);
                Map<String, Object> endData = endSession(session);
                reportId = (Long) endData.get("reportId");
                sendEvent(emitter, Map.of("type", "interview_end", "reportId", reportId,
                        "content", "面试已结束，正在生成报告..."));
                messageType = "CLOSING";
                content = decision.content();
            }
            default -> messageType = "NORMAL";
        }
        ChatMessage reply = saveMessage(session.getId(),
                decision.action() == FollowUpStrategy.Action.NEXT_QUESTION && ctx.currentOrder <= session.getTotalQuestions()
                        ? getCurrentQuestion(session.getId(), ctx.currentOrder).getQuestionId()
                        : currentQ.getId(),
                "ASSISTANT", content, messageType, ctx.currentOrder);
        Map<String, Object> done = new HashMap<>();
        done.put("type", "done");
        done.put("messageId", reply.getId());
        done.put("messageType", messageType);
        done.put("questionOrder", ctx.currentOrder);
        if (reportId != null) done.put("reportId", reportId);
        sendEvent(emitter, done);
        emitter.complete();
    }

    @Override
    @Transactional
    public Map<String, Object> end(Long userId, Long sessionId) {
        InterviewSession session = requireSession(userId, sessionId);
        return endSession(session);
    }

    private Map<String, Object> endSession(InterviewSession session) {
        if ("COMPLETED".equals(session.getSessionStatus())) {
            EvaluationReport existing = reportMapper.selectOne(new LambdaQueryWrapper<EvaluationReport>()
                    .eq(EvaluationReport::getSessionId, session.getId()));
            return buildEndResponse(session.getId(), existing);
        }
        session.setSessionStatus("COMPLETED");
        session.setEndTime(LocalDateTime.now());
        if (session.getStartTime() != null) {
            session.setDurationSeconds((int) Duration.between(session.getStartTime(), session.getEndTime()).getSeconds());
        }
        sessionMapper.updateById(session);
        EvaluationReport report = new EvaluationReport();
        report.setSessionId(session.getId());
        report.setUserId(session.getUserId());
        report.setPositionCode(session.getPositionCode());
        report.setReportStatus("GENERATING");
        reportMapper.insert(report);
        aiEvaluationService.evaluateAsync(report.getId());
        sessionContexts.remove(session.getId());
        return buildEndResponse(session.getId(), report);
    }

    @Override
    public Map<String, Object> getSession(Long userId, Long sessionId) {
        InterviewSession session = requireSession(userId, sessionId);
        Map<String, Object> m = new HashMap<>();
        m.put("sessionId", session.getId());
        m.put("positionCode", session.getPositionCode());
        m.put("sessionStatus", session.getSessionStatus());
        m.put("totalQuestions", session.getTotalQuestions());
        m.put("answeredCount", session.getAnsweredCount());
        m.put("durationSeconds", session.getDurationSeconds());
        m.put("startTime", session.getStartTime());
        m.put("endTime", session.getEndTime());
        return m;
    }

    @Override
    public Map<String, Object> getMessages(Long userId, Long sessionId) {
        requireSession(userId, sessionId);
        List<ChatMessage> messages = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getCreatedAt));
        Map<Long, Integer> questionOrderMap = interviewQuestionMapper.selectList(
                        new LambdaQueryWrapper<InterviewQuestion>().eq(InterviewQuestion::getSessionId, sessionId))
                .stream().collect(Collectors.toMap(InterviewQuestion::getQuestionId, InterviewQuestion::getQuestionOrder));
        List<Map<String, Object>> list = messages.stream().map(msg -> {
            Map<String, Object> m = new HashMap<>();
            m.put("messageId", msg.getId());
            m.put("role", msg.getRole());
            m.put("content", msg.getContent());
            m.put("messageType", msg.getMessageType());
            m.put("questionOrder", msg.getQuestionId() != null ? questionOrderMap.getOrDefault(msg.getQuestionId(), 0) : 0);
            m.put("createdAt", msg.getCreatedAt());
            return m;
        }).collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", sessionId);
        result.put("messages", list);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> codingSubmit(Long userId, Long sessionId, CodingSubmitRequest request) {
        requireSession(userId, sessionId);
        Long count = codingSubmitMapper.selectCount(new LambdaQueryWrapper<SessionCodingSubmit>()
                .eq(SessionCodingSubmit::getSessionId, sessionId)
                .eq(SessionCodingSubmit::getQuestionId, request.getQuestionId()));
        SessionCodingSubmit submit = new SessionCodingSubmit();
        submit.setSessionId(sessionId);
        submit.setQuestionId(request.getQuestionId());
        submit.setCodeBody(request.getCode());
        submit.setLanguage(request.getLanguage());
        submit.setSubmitOrder(count.intValue() + 1);
        codingSubmitMapper.insert(submit);
        Map<String, Object> m = new HashMap<>();
        m.put("submitId", submit.getId());
        m.put("submitOrder", submit.getSubmitOrder());
        return m;
    }

    private List<Question> pickQuestions(String positionCode, int count) {
        List<Question> all = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .eq(Question::getPositionCode, positionCode)
                .isNull(Question::getBindingSessionId)
                .last("LIMIT " + count * 3));
        Collections.shuffle(all);
        return all.stream().limit(count).collect(Collectors.toList());
    }

    private List<Question> createFallbackQuestions(String positionCode, int count) {
        List<Question> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Question q = new Question();
            q.setPositionCode(positionCode);
            q.setTitle("请介绍一下你在 " + positionCode + " 方向的技术积累（第" + i + "题）");
            q.setDifficulty(2);
            q.setQuestionType("TECH_KNOWLEDGE");
            q.setTopic("综合");
            q.setSource("FALLBACK");
            questionMapper.insert(q);
            list.add(q);
        }
        return list;
    }

    private InterviewQuestion getCurrentQuestion(Long sessionId, int order) {
        return interviewQuestionMapper.selectOne(new LambdaQueryWrapper<InterviewQuestion>()
                .eq(InterviewQuestion::getSessionId, sessionId)
                .eq(InterviewQuestion::getQuestionOrder, order));
    }

    private ChatMessage saveMessage(Long sessionId, Long questionId, String role, String content,
                                    String messageType, int questionOrder) {
        ChatMessage msg = new ChatMessage();
        msg.setSessionId(sessionId);
        msg.setQuestionId(questionId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setMessageType(messageType);
        chatMessageMapper.insert(msg);
        return msg;
    }

    private String buildGreeting(String positionName, int total, String firstTitle) {
        return "你好！我是今天的面试官，很高兴认识你。我们今天进行的是 "
                + positionName + " 岗位的模拟面试，共有 " + total + " 道题目，请放松心态，我们开始吧。\n\n**第一题：**"
                + firstTitle;
    }

    private InterviewSession requireSession(Long userId, Long sessionId) {
        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw BusinessException.notFound("会话不存在");
        }
        return session;
    }

    private Map<String, Object> buildEndResponse(Long sessionId, EvaluationReport report) {
        Map<String, Object> m = new HashMap<>();
        m.put("sessionId", sessionId);
        m.put("reportId", report.getId());
        m.put("reportStatus", report.getReportStatus());
        m.put("message", "面试已结束，正在生成评估报告，请稍候...");
        return m;
    }

    private void sendEvent(SseEmitter emitter, Map<String, Object> data) {
        try {
            emitter.send(SseEmitter.event().data(JSONUtil.toJsonStr(data)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class SessionContext {
        int currentOrder = 1;
        int followUpCount = 0;
    }
}
