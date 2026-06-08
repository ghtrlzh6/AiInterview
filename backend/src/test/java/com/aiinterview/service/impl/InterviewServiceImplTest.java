package com.aiinterview.service.impl;

import com.aiinterview.dto.interview.CodingSubmitRequest;
import com.aiinterview.dto.interview.EndInterviewRequest;
import com.aiinterview.dto.interview.StartInterviewRequest;
import com.aiinterview.entity.*;
import com.aiinterview.mapper.*;
import com.aiinterview.service.ai.AiEvaluationService;
import com.aiinterview.service.ai.FollowUpStrategy;
import com.aiinterview.service.ai.LlmService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InterviewServiceImplTest {

    private InterviewSessionMapper sessionMapper;
    private InterviewQuestionMapper interviewQuestionMapper;
    private QuestionMapper questionMapper;
    private ChatMessageMapper chatMessageMapper;
    private PositionMapper positionMapper;
    private UserResumeMapper resumeMapper;
    private ResumeProjectMapper resumeProjectMapper;
    private SessionCodingSubmitMapper codingSubmitMapper;
    private CodingChallengeMapper codingChallengeMapper;
    private EvaluationReportMapper reportMapper;
    private LlmService llmService;
    private InterviewServiceImpl service;

    @BeforeEach
    void setUp() {
        sessionMapper = mock(InterviewSessionMapper.class);
        interviewQuestionMapper = mock(InterviewQuestionMapper.class);
        questionMapper = mock(QuestionMapper.class);
        chatMessageMapper = mock(ChatMessageMapper.class);
        positionMapper = mock(PositionMapper.class);
        resumeMapper = mock(UserResumeMapper.class);
        resumeProjectMapper = mock(ResumeProjectMapper.class);
        codingSubmitMapper = mock(SessionCodingSubmitMapper.class);
        codingChallengeMapper = mock(CodingChallengeMapper.class);
        reportMapper = mock(EvaluationReportMapper.class);
        FollowUpStrategy followUpStrategy = mock(FollowUpStrategy.class);
        llmService = mock(LlmService.class);
        AiEvaluationService aiEvaluationService = mock(AiEvaluationService.class);
        service = new InterviewServiceImpl(
                sessionMapper,
                interviewQuestionMapper,
                questionMapper,
                chatMessageMapper,
                positionMapper,
                resumeMapper,
                resumeProjectMapper,
                codingSubmitMapper,
                codingChallengeMapper,
                reportMapper,
                followUpStrategy,
                llmService,
                aiEvaluationService);
    }

    @Test
    void startWithParsedResumeCreatesProjectQuestionMetadata() {
        Position position = new Position();
        position.setCode("JAVA_BACKEND");
        position.setName("Java鍚庣寮€鍙戝伐绋嬪笀");
        when(positionMapper.selectOne(any())).thenReturn(position);

        UserResume resume = new UserResume();
        resume.setId(10L);
        resume.setUserId(7L);
        resume.setParseStatus("SUCCESS");
        when(resumeMapper.selectById(10L)).thenReturn(resume);

        ResumeProject project = new ResumeProject();
        project.setId(20L);
        project.setProjectName("order-system");
        project.setSummaryMd("Handled order flow and inventory deduction.");
        when(resumeProjectMapper.selectOne(any())).thenReturn(project);

        AtomicLong ids = new AtomicLong(100);
        when(sessionMapper.selectOne(any())).thenReturn(null);
        when(sessionMapper.insert(any(InterviewSession.class))).thenAnswer(invocation -> {
            ((InterviewSession) invocation.getArgument(0)).setId(ids.getAndIncrement());
            return 1;
        });
        when(questionMapper.insert(any(Question.class))).thenAnswer(invocation -> {
            ((Question) invocation.getArgument(0)).setId(ids.getAndIncrement());
            return 1;
        });
        when(chatMessageMapper.insert(any(ChatMessage.class))).thenAnswer(invocation -> {
            ((ChatMessage) invocation.getArgument(0)).setId(ids.getAndIncrement());
            return 1;
        });
        when(questionMapper.selectOne(any()))
                .thenReturn(question(201L, "BEHAVIOR", "涓ゆ暟涔嬪拰"))
                .thenReturn(question(202L, "SCENARIO", "绉掓潃绯荤粺璁捐"))
                .thenReturn(question(203L, "TECH_KNOWLEDGE", "JVM 鍐呭瓨妯″瀷"));
        when(questionMapper.selectList(any())).thenReturn(List.of());

        StartInterviewRequest request = new StartInterviewRequest();
        request.setPositionCode("JAVA_BACKEND");
        request.setQuestionCount(4);
        request.setResumeSnapshotId(10L);

        Map<String, Object> result = service.start(7L, request);

        assertThat(result).containsEntry("positionCode", "JAVA_BACKEND");
        Map<String, Object> currentQuestion = castMap(result.get("currentQuestion"));
        assertThat(currentQuestion).containsEntry("questionType", "PROJECT_DEEP");
        assertThat((String) currentQuestion.get("questionTitle")).contains("order-system");
        Map<String, Object> firstMessage = castMap(result.get("firstMessage"));
        assertThat(firstMessage).containsEntry("questionType", "PROJECT_DEEP");
    }

    @Test
    void startRejectsWhenUserHasActiveSession() {
        InterviewSession active = new InterviewSession();
        active.setId(88L);
        active.setUserId(7L);
        active.setSessionStatus("IN_PROGRESS");
        when(sessionMapper.selectOne(any())).thenReturn(active);

        StartInterviewRequest request = new StartInterviewRequest();
        request.setPositionCode("JAVA_BACKEND");

        assertThatThrownBy(() -> service.start(7L, request))
                .isInstanceOf(RuntimeException.class);
        verify(positionMapper, never()).selectOne(any());
    }

    @Test
    void getActiveSessionReturnsCurrentQuestion() {
        InterviewSession active = new InterviewSession();
        active.setId(88L);
        active.setUserId(7L);
        active.setPositionCode("JAVA_BACKEND");
        active.setSessionStatus("IN_PROGRESS");
        active.setInputMode("TEXT");
        active.setTotalQuestions(3);
        when(sessionMapper.selectOne(any())).thenReturn(active);

        Position position = new Position();
        position.setCode("JAVA_BACKEND");
        position.setName("Java Backend");
        when(positionMapper.selectOne(any())).thenReturn(position);

        InterviewQuestion current = new InterviewQuestion();
        current.setSessionId(88L);
        current.setQuestionId(2L);
        current.setQuestionOrder(2);
        when(interviewQuestionMapper.selectOne(any())).thenReturn(current);
        when(questionMapper.selectById(2L)).thenReturn(question(2L, "SCENARIO", "璁捐璁㈠崟绯荤粺"));

        Map<String, Object> result = service.getActiveSession(7L);

        assertThat(result).containsEntry("active", true)
                .containsEntry("sessionId", 88L)
                .containsEntry("positionName", "Java Backend");
        Map<String, Object> currentQuestion = castMap(result.get("currentQuestion"));
        assertThat(currentQuestion).containsEntry("questionOrder", 2);
    }

    @Test
    void endCanSkipReportGeneration() {
        InterviewSession session = new InterviewSession();
        session.setId(1L);
        session.setUserId(7L);
        session.setSessionStatus("IN_PROGRESS");
        when(sessionMapper.selectById(1L)).thenReturn(session);

        EndInterviewRequest request = new EndInterviewRequest();
        request.setGenerateReport(false);

        Map<String, Object> result = service.end(7L, 1L, request);

        assertThat(result).containsEntry("sessionId", 1L)
                .containsEntry("reportStatus", "NOT_GENERATED");
        assertThat(result.get("reportId")).isNull();
        verify(reportMapper, never()).insert(any(EvaluationReport.class));
        verify(sessionMapper).updateById(session);
    }

    @Test
    void generateReportForCompletedSessionCreatesReport() {
        InterviewSession session = new InterviewSession();
        session.setId(1L);
        session.setUserId(7L);
        session.setPositionCode("JAVA_BACKEND");
        session.setSessionStatus("COMPLETED");
        when(sessionMapper.selectById(1L)).thenReturn(session);
        when(reportMapper.selectOne(any())).thenReturn(null);
        when(reportMapper.insert(any(EvaluationReport.class))).thenAnswer(invocation -> {
            ((EvaluationReport) invocation.getArgument(0)).setId(99L);
            return 1;
        });

        Map<String, Object> result = service.generateReport(7L, 1L);

        assertThat(result).containsEntry("sessionId", 1L)
                .containsEntry("reportId", 99L)
                .containsEntry("reportStatus", "GENERATING");
        verify(reportMapper).insert(any(EvaluationReport.class));
    }

    @Test
    void codingSubmitSavesUserMessageWithoutAutoReview() {
        InterviewSession session = new InterviewSession();
        session.setId(1L);
        session.setUserId(7L);
        session.setSessionStatus("IN_PROGRESS");
        when(sessionMapper.selectById(1L)).thenReturn(session);

        InterviewQuestion iq = new InterviewQuestion();
        iq.setSessionId(1L);
        iq.setQuestionId(2L);
        iq.setQuestionOrder(1);
        when(interviewQuestionMapper.selectOne(any())).thenReturn(iq);
        when(questionMapper.selectById(2L)).thenReturn(question(2L, "BEHAVIOR", "two sum"));
        when(codingSubmitMapper.selectCount(any())).thenReturn(0L);
        when(codingSubmitMapper.insert(any(SessionCodingSubmit.class))).thenAnswer(invocation -> {
            ((SessionCodingSubmit) invocation.getArgument(0)).setId(9L);
            return 1;
        });

        CodingSubmitRequest request = new CodingSubmitRequest();
        request.setQuestionId(2L);
        request.setLanguage("java");
        request.setCode("class Solution { int[] twoSum(int[] nums, int target) { for (int i=0;i<nums.length;i++){} return new int[0]; } }");

        Map<String, Object> result = service.codingSubmit(7L, 1L, request);

        assertThat(result).containsEntry("submitId", 9L).containsEntry("submitOrder", 1);
        assertThat(result).containsKey("message").containsKey("followUpSuggestion");
        assertThat(result).doesNotContainKey("review");
        verify(codingSubmitMapper).insert(any(SessionCodingSubmit.class));
        verify(chatMessageMapper).insert(argThat((ChatMessage msg) ->
                "USER".equals(msg.getRole())
                        && "CODING_SUBMIT".equals(msg.getMessageType())
                        && msg.getContent().contains("class Solution")));
        verifyNoInteractions(llmService);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    private Question question(Long id, String type, String title) {
        Question question = new Question();
        question.setId(id);
        question.setPositionCode("JAVA_BACKEND");
        question.setQuestionType(type);
        question.setTitle(title);
        question.setDifficulty(2);
        question.setTopic("缁煎悎");
        return question;
    }
}
