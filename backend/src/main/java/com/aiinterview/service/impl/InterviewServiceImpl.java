package com.aiinterview.service.impl;

import cn.hutool.json.JSONUtil;
import com.aiinterview.common.BusinessException;
import com.aiinterview.dto.interview.CodingSubmitRequest;
import com.aiinterview.dto.interview.EndInterviewRequest;
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
    private final ResumeProjectMapper resumeProjectMapper;
    private final SessionCodingSubmitMapper codingSubmitMapper;
    private final CodingChallengeMapper codingChallengeMapper;
    private final EvaluationReportMapper reportMapper;
    private final FollowUpStrategy followUpStrategy;
    private final LlmService llmService;
    private final AiEvaluationService aiEvaluationService;

    private final Map<Long, SessionContext> sessionContexts = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public Map<String, Object> start(Long userId, StartInterviewRequest request) {
        InterviewSession active = findActiveSession(userId);
        if (active != null) {
            throw new BusinessException("还有未完成的面试，请先继续或结束当前面试");
        }
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
        InterviewSession session = new InterviewSession();
        session.setUserId(userId);
        session.setResumeSnapshotId(request.getResumeSnapshotId());
        session.setPositionCode(request.getPositionCode());
        session.setSessionStatus("IN_PROGRESS");
        session.setInputMode(StringUtils.hasText(request.getInputMode()) ? request.getInputMode() : "TEXT");
        session.setTotalQuestions(0);
        session.setAnsweredCount(0);
        session.setStartTime(LocalDateTime.now());
        sessionMapper.insert(session);
        List<Question> questions = pickQuestions(request.getPositionCode(), count, session.getId(), request.getResumeSnapshotId());
        if (questions.isEmpty()) {
            questions = createFallbackQuestions(request.getPositionCode(), count);
        }
        session.setTotalQuestions(questions.size());
        sessionMapper.updateById(session);
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
        Map<String, Object> currentQuestion = buildQuestionMeta(firstQ, 1);
        Map<String, Object> msg = buildMessageMeta(firstMsg, firstQ, 1);
        data.put("firstMessage", msg);
        data.put("currentQuestion", currentQuestion);
        return data;
    }

    @Override
    public Map<String, Object> getActiveSession(Long userId) {
        InterviewSession active = findActiveSession(userId);
        if (active == null) {
            return Map.of("active", false);
        }
        Map<String, Object> detail = buildSessionDetail(active);
        detail.put("active", true);
        return detail;
    }

    @Override
    public SseEmitter sendMessage(Long userId, Long sessionId, SendMessageRequest request) {
        InterviewSession session = requireSession(userId, sessionId);
        if (!"IN_PROGRESS".equals(session.getSessionStatus())) {
            throw new BusinessException("面试已结束");
        }
        SessionContext ctx = sessionContexts.computeIfAbsent(sessionId, k -> rebuildSessionContext(session));
        InterviewQuestion currentIq = getCurrentQuestion(sessionId, ctx.currentOrder);
        if (currentIq == null) {
            throw new BusinessException("当前面试题目不存在，请结束本次面试后重新开始");
        }
        Question currentQ = questionMapper.selectById(currentIq.getQuestionId());
        saveMessage(sessionId, currentQ.getId(), "USER", request.getContent(), request.getMessageType(), ctx.currentOrder);
        Position position = positionMapper.selectOne(new LambdaQueryWrapper<Position>()
                .eq(Position::getCode, session.getPositionCode()));
        FollowUpStrategy.Decision decision = followUpStrategy.decide(
                currentQ, request.getContent(), ctx.followUpCount,
                ctx.currentOrder, session.getTotalQuestions(),
                position != null ? position.getName() : session.getPositionCode(),
                latestCodingContext(sessionId, currentQ));
        SseEmitter emitter = new SseEmitter(120000L);
        new Thread(() -> {
            try {
                streamResponse(emitter, decision, session, ctx, currentQ, currentIq);
            } catch (Exception e) {
                log.error("SSE error", e);
                Map<String, Object> errorEvent = new HashMap<>();
                errorEvent.put("type", "error");
                errorEvent.put("content", StringUtils.hasText(e.getMessage()) ? e.getMessage() : "SSE response failed");
                sendEvent(emitter, errorEvent);
                emitter.completeWithError(e);
            }
        }).start();
        return emitter;
    }

    private void streamResponse(SseEmitter emitter, FollowUpStrategy.Decision decision,
                                InterviewSession session, SessionContext ctx,
                                Question currentQ, InterviewQuestion currentIq) throws IOException {
        String content = decision.content();
        String messageType;
        Long reportId = null;
        switch (decision.action()) {
            case FOLLOW_UP -> {
                ctx.followUpCount++;
                messageType = "FOLLOW_UP";
                sendEvent(emitter, Map.of("type", "token", "content", content));
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
                    Map<String, Object> event = new HashMap<>(buildQuestionMeta(nextQ, ctx.currentOrder));
                    event.put("type", "next_question");
                    event.put("content", content);
                    sendEvent(emitter, event);
                }
            }
            case END -> {
                currentIq.setIsAnswered(1);
                interviewQuestionMapper.updateById(currentIq);
                Map<String, Object> endData = endSession(session, true);
                reportId = (Long) endData.get("reportId");
                Map<String, Object> endEvent = new HashMap<>();
                endEvent.put("type", "interview_end");
                endEvent.put("content", endData.getOrDefault("message", "Interview completed, generating report..."));
                if (reportId != null) {
                    endEvent.put("reportId", reportId);
                }
                sendEvent(emitter, endEvent);
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
        Question activeQuestion = resolveQuestionForOrder(session.getId(), ctx.currentOrder);
        if (activeQuestion != null) {
            done.putAll(buildQuestionMeta(activeQuestion, ctx.currentOrder));
        }
        if (reportId != null) done.put("reportId", reportId);
        sendEvent(emitter, done);
        emitter.complete();
    }

    @Override
    @Transactional
    public Map<String, Object> end(Long userId, Long sessionId, EndInterviewRequest request) {
        InterviewSession session = requireSession(userId, sessionId);
        boolean generateReport = request == null || !Boolean.FALSE.equals(request.getGenerateReport());
        return endSession(session, generateReport);
    }

    @Override
    @Transactional
    public Map<String, Object> generateReport(Long userId, Long sessionId) {
        InterviewSession session = requireSession(userId, sessionId);
        if ("IN_PROGRESS".equals(session.getSessionStatus())) {
            endSession(session, false);
        }
        EvaluationReport report = generateReportForSession(session);
        return buildEndResponse(session.getId(), report);
    }

    private Map<String, Object> endSession(InterviewSession session, boolean generateReport) {
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
        sessionContexts.remove(session.getId());
        EvaluationReport report = generateReport ? generateReportForSession(session) : null;
        return buildEndResponse(session.getId(), report);
    }

    private EvaluationReport generateReportForSession(InterviewSession session) {
        EvaluationReport existing = reportMapper.selectOne(new LambdaQueryWrapper<EvaluationReport>()
                .eq(EvaluationReport::getSessionId, session.getId()));
        if (existing != null) {
            return existing;
        }
        EvaluationReport report = new EvaluationReport();
        report.setSessionId(session.getId());
        report.setUserId(session.getUserId());
        report.setPositionCode(session.getPositionCode());
        report.setReportStatus("GENERATING");
        reportMapper.insert(report);
        aiEvaluationService.evaluateAsync(report.getId());
        return report;
    }

    @Override
    public Map<String, Object> getSession(Long userId, Long sessionId) {
        InterviewSession session = requireSession(userId, sessionId);
        return buildSessionDetail(session);
    }

    private Map<String, Object> buildSessionDetail(InterviewSession session) {
        Map<String, Object> m = new HashMap<>();
        m.put("sessionId", session.getId());
        m.put("positionCode", session.getPositionCode());
        Position pos = positionMapper.selectOne(new LambdaQueryWrapper<Position>()
                .eq(Position::getCode, session.getPositionCode()));
        m.put("positionName", pos != null ? pos.getName() : session.getPositionCode());
        m.put("sessionStatus", session.getSessionStatus());
        m.put("inputMode", session.getInputMode());
        m.put("totalQuestions", session.getTotalQuestions());
        m.put("answeredCount", session.getAnsweredCount());
        m.put("durationSeconds", session.getDurationSeconds());
        m.put("startTime", session.getStartTime());
        m.put("endTime", session.getEndTime());
        int currentOrder = currentOrderFromDb(session.getId(), session.getTotalQuestions());
        Question currentQuestion = resolveQuestionForOrder(session.getId(), currentOrder);
        if (currentQuestion != null) {
            m.put("currentQuestion", buildQuestionMeta(currentQuestion, currentOrder));
        }
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
        Map<Long, Question> questionMap = questionOrderMap.keySet().isEmpty()
                ? Map.of()
                : questionMapper.selectBatchIds(questionOrderMap.keySet()).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));
        List<Map<String, Object>> list = messages.stream().map(msg -> {
            Map<String, Object> m = new HashMap<>();
            m.put("messageId", msg.getId());
            m.put("role", msg.getRole());
            m.put("content", msg.getContent());
            m.put("messageType", msg.getMessageType());
            m.put("questionOrder", msg.getQuestionId() != null ? questionOrderMap.getOrDefault(msg.getQuestionId(), 0) : 0);
            if (msg.getQuestionId() != null && questionMap.containsKey(msg.getQuestionId())) {
                Question q = questionMap.get(msg.getQuestionId());
                m.put("questionId", q.getId());
                m.put("questionType", q.getQuestionType());
                m.put("questionTitle", q.getTitle());
                m.put("topic", q.getTopic());
            }
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
        Question question = requireSessionQuestion(sessionId, request.getQuestionId());
        if (!"BEHAVIOR".equals(question.getQuestionType())) {
            throw new BusinessException("当前题目不是手撕代码题");
        }
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
        String followUpSuggestion = "代码已同步到左侧对话。你可以继续补充算法思路、复杂度和边界条件，面试官会基于这次提交追问。";
        saveMessage(sessionId, question.getId(), "USER",
                buildCodingSubmitMessage(submit, request.getCode()),
                "CODING_SUBMIT",
                resolveQuestionOrder(sessionId, question.getId()));
        Map<String, Object> m = new HashMap<>();
        m.put("submitId", submit.getId());
        m.put("submitOrder", submit.getSubmitOrder());
        m.put("questionId", question.getId());
        m.put("language", submit.getLanguage());
        m.put("message", "代码已提交并同步到对话");
        m.put("followUpSuggestion", followUpSuggestion);
        m.put("createdAt", submit.getCreatedAt());
        return m;
    }

    @Override
    public Map<String, Object> latestCodingSubmit(Long userId, Long sessionId, Long questionId) {
        requireSession(userId, sessionId);
        requireSessionQuestion(sessionId, questionId);
        SessionCodingSubmit submit = codingSubmitMapper.selectOne(new LambdaQueryWrapper<SessionCodingSubmit>()
                .eq(SessionCodingSubmit::getSessionId, sessionId)
                .eq(SessionCodingSubmit::getQuestionId, questionId)
                .orderByDesc(SessionCodingSubmit::getSubmitOrder)
                .last("LIMIT 1"));
        if (submit == null) {
            return Map.of("submitted", false);
        }
        Map<String, Object> m = new HashMap<>();
        m.put("submitted", true);
        m.put("submitId", submit.getId());
        m.put("submitOrder", submit.getSubmitOrder());
        m.put("questionId", submit.getQuestionId());
        m.put("language", submit.getLanguage());
        m.put("code", submit.getCodeBody());
        m.put("createdAt", submit.getCreatedAt());
        return m;
    }

    private String latestCodingContext(Long sessionId, Question question) {
        if (question == null || !"BEHAVIOR".equals(question.getQuestionType())) {
            return "";
        }
        SessionCodingSubmit submit = codingSubmitMapper.selectOne(new LambdaQueryWrapper<SessionCodingSubmit>()
                .eq(SessionCodingSubmit::getSessionId, sessionId)
                .eq(SessionCodingSubmit::getQuestionId, question.getId())
                .orderByDesc(SessionCodingSubmit::getSubmitOrder)
                .last("LIMIT 1"));
        if (submit == null || !StringUtils.hasText(submit.getCodeBody())) {
            return "";
        }
        String code = submit.getCodeBody();
        if (code.length() > 2000) {
            code = code.substring(0, 2000) + "\n...";
        }
        return "语言：" + submit.getLanguage()
                + "\n提交次数：" + submit.getSubmitOrder()
                + "\n代码：\n" + code;
    }

    private List<Question> pickQuestions(String positionCode, int count, Long sessionId, Long resumeSnapshotId) {
        List<Question> selected = new ArrayList<>();
        if (resumeSnapshotId != null) {
            Question projectQuestion = createProjectDeepQuestion(positionCode, sessionId, resumeSnapshotId);
            if (projectQuestion != null) {
                selected.add(projectQuestion);
            }
        }
        addOneByType(selected, positionCode, "BEHAVIOR");
        addOneByType(selected, positionCode, "SCENARIO");
        addOneByType(selected, positionCode, "TECH_KNOWLEDGE");

        List<Question> all = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .eq(Question::getPositionCode, positionCode)
                .isNull(Question::getBindingSessionId)
                .last("LIMIT " + Math.max(count * 4, 20)));
        Collections.shuffle(all);
        Set<Long> selectedIds = selected.stream()
                .map(Question::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        for (Question question : all) {
            if (selected.size() >= count) {
                break;
            }
            if (question.getId() != null && selectedIds.add(question.getId())) {
                selected.add(question);
            }
        }
        return selected.stream().limit(count).collect(Collectors.toList());
    }

    private void addOneByType(List<Question> selected, String positionCode, String questionType) {
        Question question = questionMapper.selectOne(new LambdaQueryWrapper<Question>()
                .eq(Question::getPositionCode, positionCode)
                .eq(Question::getQuestionType, questionType)
                .isNull(Question::getBindingSessionId)
                .last("LIMIT 1"));
        if (question != null && selected.stream().noneMatch(q -> Objects.equals(q.getId(), question.getId()))) {
            selected.add(question);
        }
    }

    private Question createProjectDeepQuestion(String positionCode, Long sessionId, Long resumeSnapshotId) {
        ResumeProject project = resumeProjectMapper.selectOne(new LambdaQueryWrapper<ResumeProject>()
                .eq(ResumeProject::getResumeId, resumeSnapshotId)
                .orderByAsc(ResumeProject::getSortOrder)
                .last("LIMIT 1"));
        if (project == null) {
            return null;
        }
        String title = "请结合简历中的「" + project.getProjectName()
                + "」项目，深入讲解一个你亲自解决的技术挑战，并说明方案权衡。";
        Question q = new Question();
        q.setPositionCode(positionCode);
        q.setBindingSessionId(sessionId);
        q.setTitle(title);
        q.setAnswerReference("结合简历项目背景、技术栈、个人职责、问题定位、方案取舍、落地效果和复盘总结展开。");
        q.setDifficulty(2);
        q.setQuestionType("PROJECT_DEEP");
        q.setTopic("简历项目");
        q.setSource("AI_PROJECT_FALLBACK");
        Map<String, Object> meta = new HashMap<>();
        meta.put("resumeSnapshotId", resumeSnapshotId);
        meta.put("resumeProjectId", project.getId());
        meta.put("projectName", project.getProjectName());
        meta.put("summary", project.getSummaryMd());
        q.setGenerationMeta(meta);
        questionMapper.insert(q);
        return q;
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

    private InterviewSession findActiveSession(Long userId) {
        return sessionMapper.selectOne(new LambdaQueryWrapper<InterviewSession>()
                .eq(InterviewSession::getUserId, userId)
                .eq(InterviewSession::getSessionStatus, "IN_PROGRESS")
                .orderByDesc(InterviewSession::getStartTime)
                .last("LIMIT 1"));
    }

    private SessionContext rebuildSessionContext(InterviewSession session) {
        SessionContext ctx = new SessionContext();
        ctx.currentOrder = currentOrderFromDb(session.getId(), session.getTotalQuestions());
        ctx.followUpCount = countFollowUps(session.getId(), ctx.currentOrder);
        return ctx;
    }

    private int currentOrderFromDb(Long sessionId, Integer totalQuestions) {
        InterviewQuestion current = interviewQuestionMapper.selectOne(new LambdaQueryWrapper<InterviewQuestion>()
                .eq(InterviewQuestion::getSessionId, sessionId)
                .eq(InterviewQuestion::getIsAnswered, 0)
                .orderByAsc(InterviewQuestion::getQuestionOrder)
                .last("LIMIT 1"));
        if (current != null && current.getQuestionOrder() != null) {
            return current.getQuestionOrder();
        }
        return Math.max(totalQuestions == null ? 1 : totalQuestions, 1);
    }

    private int countFollowUps(Long sessionId, int currentOrder) {
        InterviewQuestion current = getCurrentQuestion(sessionId, currentOrder);
        if (current == null || current.getQuestionId() == null) {
            return 0;
        }
        Long count = chatMessageMapper.selectCount(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .eq(ChatMessage::getQuestionId, current.getQuestionId())
                .eq(ChatMessage::getRole, "ASSISTANT")
                .eq(ChatMessage::getMessageType, "FOLLOW_UP"));
        return count == null ? 0 : count.intValue();
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

    private int resolveQuestionOrder(Long sessionId, Long questionId) {
        InterviewQuestion iq = interviewQuestionMapper.selectOne(new LambdaQueryWrapper<InterviewQuestion>()
                .eq(InterviewQuestion::getSessionId, sessionId)
                .eq(InterviewQuestion::getQuestionId, questionId)
                .last("LIMIT 1"));
        return iq != null && iq.getQuestionOrder() != null ? iq.getQuestionOrder() : 0;
    }

    private String buildCodingSubmitMessage(SessionCodingSubmit submit, String code) {
        String codePreview = code;
        if (codePreview != null && codePreview.length() > 1200) {
            codePreview = codePreview.substring(0, 1200) + "\n...";
        }
        return "**代码提交（第 " + submit.getSubmitOrder() + " 次，" + submit.getLanguage() + "）**\n\n"
                + "```" + submit.getLanguage() + "\n"
                + (codePreview == null ? "" : codePreview)
                + "\n```";
    }

    private Map<String, Object> buildMessageMeta(ChatMessage message, Question question, int questionOrder) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("messageId", message.getId());
        msg.put("role", message.getRole());
        msg.put("content", message.getContent());
        msg.put("messageType", message.getMessageType());
        msg.put("questionOrder", questionOrder);
        msg.putAll(buildQuestionMeta(question, questionOrder));
        return msg;
    }

    private Map<String, Object> buildQuestionMeta(Question question, int questionOrder) {
        Map<String, Object> m = new HashMap<>();
        m.put("questionId", question.getId());
        m.put("questionOrder", questionOrder);
        m.put("questionType", question.getQuestionType());
        m.put("questionTitle", question.getTitle());
        m.put("topic", question.getTopic());
        if ("BEHAVIOR".equals(question.getQuestionType()) && question.getCodingChallengeId() != null) {
            CodingChallenge challenge = codingChallengeMapper.selectById(question.getCodingChallengeId());
            if (challenge != null) {
                Map<String, Object> challengeMap = new HashMap<>();
                challengeMap.put("id", challenge.getId());
                challengeMap.put("title", challenge.getTitle());
                challengeMap.put("problemMd", challenge.getProblemMd());
                challengeMap.put("difficulty", challenge.getDifficulty());
                challengeMap.put("tags", challenge.getCanonicalTags());
                m.put("codingChallenge", challengeMap);
            }
        }
        return m;
    }

    private Question resolveQuestionForOrder(Long sessionId, int order) {
        InterviewQuestion iq = getCurrentQuestion(sessionId, order);
        return iq == null ? null : questionMapper.selectById(iq.getQuestionId());
    }

    private Question requireSessionQuestion(Long sessionId, Long questionId) {
        InterviewQuestion iq = interviewQuestionMapper.selectOne(new LambdaQueryWrapper<InterviewQuestion>()
                .eq(InterviewQuestion::getSessionId, sessionId)
                .eq(InterviewQuestion::getQuestionId, questionId)
                .last("LIMIT 1"));
        if (iq == null) {
            throw BusinessException.notFound("题目不属于当前会话");
        }
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw BusinessException.notFound("题目不存在");
        }
        return question;
    }

    private String reviewCode(Question question, CodingSubmitRequest request) {
        if (!StringUtils.hasText(request.getCode()) || request.getCode().trim().length() < 20) {
            return "代码内容较短，请补充完整实现，并说明核心思路、复杂度和关键边界条件。";
        }
        if (!llmService.isAvailable()) {
            return mockCodeReview(request.getCode());
        }
        try {
            String prompt = "你是代码面试官。请基于题目和候选人代码给出简短评审，指出正确思路、复杂度和一个追问点。"
                    + "\n题目：" + question.getTitle()
                    + "\n语言：" + request.getLanguage()
                    + "\n代码：\n" + request.getCode();
            String raw = llmService.chat(List.of(new LlmService.ChatMessage("user", prompt)));
            return StringUtils.hasText(raw) ? raw : mockCodeReview(request.getCode());
        } catch (Exception e) {
            log.warn("Code review failed, fallback to mock", e);
            return mockCodeReview(request.getCode());
        }
    }

    private String mockCodeReview(String code) {
        String complexityHint = code.contains("for") || code.contains("while")
                ? "代码中已经体现了遍历思路，请注意说明时间复杂度和边界条件。"
                : "请在讲解时补充主要控制流程和复杂度分析。";
        return "代码已保存。初步评审：整体提交可用于继续面试，" + complexityHint
                + "下一步建议说明为什么选择该算法，以及空输入、重复元素或极端规模下如何处理。";
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
        if (report != null) {
            m.put("reportId", report.getId());
            m.put("reportStatus", report.getReportStatus());
            m.put("message", "面试已结束，正在生成评估报告，请稍候...");
        } else {
            m.put("reportId", null);
            m.put("reportStatus", "NOT_GENERATED");
            m.put("message", "面试已结束，可稍后在首页生成报告");
        }
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
