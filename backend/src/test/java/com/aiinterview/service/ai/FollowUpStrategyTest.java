package com.aiinterview.service.ai;

import com.aiinterview.entity.Question;
import com.aiinterview.service.PromptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FollowUpStrategyTest {

    private LlmService llmService;
    private FollowUpStrategy strategy;

    @BeforeEach
    void setUp() {
        llmService = mock(LlmService.class);
        PromptService promptService = mock(PromptService.class);
        when(promptService.getInterviewPrompt("JAVA_BACKEND")).thenReturn("面试官提示词");
        strategy = new FollowUpStrategy(llmService, promptService);
    }

    @Test
    void skipIntentMovesToNextQuestionWithoutCallingLlm() {
        FollowUpStrategy.Decision decision = strategy.decide(
                question(),
                "这个问题不会，能到下一题吗？",
                0,
                1,
                3,
                "Java Backend",
                "",
                "JAVA_BACKEND");

        assertThat(decision.action()).isEqualTo(FollowUpStrategy.Action.NEXT_QUESTION);
    }

    @Test
    void skipIntentEndsWhenCurrentQuestionIsLast() {
        FollowUpStrategy.Decision decision = strategy.decide(
                question(),
                "跳过",
                0,
                3,
                3,
                "Java Backend",
                "",
                "JAVA_BACKEND");

        assertThat(decision.action()).isEqualTo(FollowUpStrategy.Action.END);
    }

    @Test
    void parsesJsonWithReplyField() {
        when(llmService.chatJson(any())).thenReturn(
                "{\"action\":\"follow_up\",\"reply\":\"你讲得不错，能再举一个实际项目例子吗？\"}");

        FollowUpStrategy.Decision decision = decideWithLongAnswer();

        assertThat(decision.action()).isEqualTo(FollowUpStrategy.Action.FOLLOW_UP);
        assertThat(decision.reply()).contains("实际项目例子");
    }

    @Test
    void parsesJsonWithLegacyContentField() {
        when(llmService.chatJson(any())).thenReturn(
                "{\"action\":\"next_question\",\"content\":\"好的，我了解了，我们继续下一题。\"}");

        FollowUpStrategy.Decision decision = decideWithLongAnswer();

        assertThat(decision.action()).isEqualTo(FollowUpStrategy.Action.NEXT_QUESTION);
        assertThat(decision.reply()).contains("继续下一题");
    }

    @Test
    void parsesMarkdownWrappedJson() {
        when(llmService.chatJson(any())).thenReturn("""
                ```json
                {"action":"follow_up","reply":"思路基本正确，但还可以再补充一下边界情况。"}
                ```
                """);

        FollowUpStrategy.Decision decision = decideWithLongAnswer();

        assertThat(decision.action()).isEqualTo(FollowUpStrategy.Action.FOLLOW_UP);
        assertThat(decision.reply()).contains("边界情况");
    }

    @Test
    void recoversWhenJsonHasExtraText() {
        when(llmService.chatJson(any())).thenReturn("""
                好的，我来判断一下。
                {"action":"follow_up","reply":"可以再具体说说你在项目里是怎么落地的吗？"}
                """);

        FollowUpStrategy.Decision decision = decideWithLongAnswer();

        assertThat(decision.action()).isEqualTo(FollowUpStrategy.Action.FOLLOW_UP);
        assertThat(decision.reply()).contains("怎么落地");
    }

    @Test
    void usesGracefulFallbackWhenResponseIsNotJson() {
        when(llmService.chatJson(any())).thenReturn("这是一段纯文本，没有 JSON。");

        FollowUpStrategy.Decision decision = decideWithLongAnswer();

        assertThat(decision.action()).isEqualTo(FollowUpStrategy.Action.FOLLOW_UP);
        assertThat(decision.reply()).doesNotContain("我没太跟上");
    }

    private FollowUpStrategy.Decision decideWithLongAnswer() {
        return strategy.decide(
                question(),
                "JVM 堆内存主要存放对象实例，栈内存存放局部变量和方法调用帧，方法区存放类元数据。",
                0,
                1,
                3,
                "Java Backend",
                "",
                "JAVA_BACKEND");
    }

    private Question question() {
        Question question = new Question();
        question.setId(1L);
        question.setTitle("Explain JVM memory model");
        question.setQuestionType("TECH_KNOWLEDGE");
        return question;
    }
}
