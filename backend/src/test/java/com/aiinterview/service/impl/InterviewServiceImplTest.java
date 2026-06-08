package com.aiinterview.service.impl;

import com.aiinterview.dto.interview.CodingSubmitRequest;
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
        position.setName("Java后端开发工程师");
        when(positionMapper.selectOne(any())).thenReturn(position);

        UserResume resume = new UserResume();
        resume.setId(10L);
        resume.setUserId(7L);
        resume.setParseStatus("SUCCESS");
        when(resumeMapper.selectById(10L)).thenReturn(resume);

        ResumeProject project = new ResumeProject();
        project.setId(20L);
        project.setProjectName("订单系统");
        project.setSummaryMd("负责订单核心链路与库存扣减。");
        when(resumeProjectMapper.selectOne(any())).thenReturn(project);

        AtomicLong ids = new AtomicLong(100);
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
                .thenReturn(question(201L, "BEHAVIOR", "两数之和"))
                .thenReturn(question(202L, "SCENARIO", "秒杀系统设计"))
                .thenReturn(question(203L, "TECH_KNOWLEDGE", "JVM 内存模型"));
        when(questionMapper.selectList(any())).thenReturn(List.of());

        StartInterviewRequest request = new StartInterviewRequest();
        request.setPositionCode("JAVA_BACKEND");
        request.setQuestionCount(4);
        request.setResumeSnapshotId(10L);

        Map<String, Object> result = service.start(7L, request);

        assertThat(result).containsEntry("positionCode", "JAVA_BACKEND");
        Map<String, Object> currentQuestion = castMap(result.get("currentQuestion"));
        assertThat(currentQuestion).containsEntry("questionType", "PROJECT_DEEP");
        assertThat((String) currentQuestion.get("questionTitle")).contains("订单系统");
        Map<String, Object> firstMessage = castMap(result.get("firstMessage"));
        assertThat(firstMessage).containsEntry("questionType", "PROJECT_DEEP");
    }

    @Test
    void codingSubmitForBehaviorQuestionReturnsMockReview() {
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
        when(questionMapper.selectById(2L)).thenReturn(question(2L, "BEHAVIOR", "请实现两数之和"));
        when(codingSubmitMapper.selectCount(any())).thenReturn(0L);
        when(codingSubmitMapper.insert(any(SessionCodingSubmit.class))).thenAnswer(invocation -> {
            ((SessionCodingSubmit) invocation.getArgument(0)).setId(9L);
            return 1;
        });
        when(llmService.isAvailable()).thenReturn(false);

        CodingSubmitRequest request = new CodingSubmitRequest();
        request.setQuestionId(2L);
        request.setLanguage("java");
        request.setCode("class Solution { int[] twoSum(int[] nums, int target) { for (int i=0;i<nums.length;i++){} return new int[0]; } }");

        Map<String, Object> result = service.codingSubmit(7L, 1L, request);

        assertThat(result).containsEntry("submitId", 9L).containsEntry("submitOrder", 1);
        assertThat((String) result.get("review")).contains("代码已保存");
        verify(codingSubmitMapper).insert(any(SessionCodingSubmit.class));
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
        question.setTopic("综合");
        return question;
    }
}
