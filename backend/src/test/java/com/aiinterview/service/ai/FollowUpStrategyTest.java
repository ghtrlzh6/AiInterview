package com.aiinterview.service.ai;

import com.aiinterview.entity.Question;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class FollowUpStrategyTest {

    @Test
    void skipIntentMovesToNextQuestionWithoutCallingLlm() {
        LlmService llmService = mock(LlmService.class);
        FollowUpStrategy strategy = new FollowUpStrategy(llmService);

        FollowUpStrategy.Decision decision = strategy.decide(
                question(),
                "这个问题不会，能到下一题吗？",
                0,
                1,
                3,
                "Java Backend");

        assertThat(decision.action()).isEqualTo(FollowUpStrategy.Action.NEXT_QUESTION);
        verifyNoInteractions(llmService);
    }

    @Test
    void skipIntentEndsWhenCurrentQuestionIsLast() {
        LlmService llmService = mock(LlmService.class);
        FollowUpStrategy strategy = new FollowUpStrategy(llmService);

        FollowUpStrategy.Decision decision = strategy.decide(
                question(),
                "跳过",
                0,
                3,
                3,
                "Java Backend");

        assertThat(decision.action()).isEqualTo(FollowUpStrategy.Action.END);
        verifyNoInteractions(llmService);
    }

    private Question question() {
        Question question = new Question();
        question.setId(1L);
        question.setTitle("Explain JVM memory model");
        question.setQuestionType("TECH_KNOWLEDGE");
        return question;
    }
}
