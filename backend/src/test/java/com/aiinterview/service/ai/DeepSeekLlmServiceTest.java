package com.aiinterview.service.ai;

import com.aiinterview.service.SystemConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeepSeekLlmServiceTest {

    @Mock
    private SystemConfigService systemConfigService;

    @InjectMocks
    private DeepSeekLlmService llmService;

    @Test
    void isAvailableFalseWhenApiKeyMissing() {
        when(systemConfigService.get("ai.llm.api-key", "")).thenReturn("");
        assertThat(llmService.isAvailable()).isFalse();
    }

    @Test
    void isAvailableTrueWhenApiKeyPresent() {
        when(systemConfigService.get("ai.llm.api-key", "")).thenReturn("sk-test-key");
        assertThat(llmService.isAvailable()).isTrue();
    }

    @Test
    void testConnectionReturnsMockModeWithoutApiKey() {
        when(systemConfigService.get("ai.llm.api-key", "")).thenReturn("");
        LlmService.LlmTestResult result = llmService.testConnection();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getModel()).isEqualTo("mock");
        assertThat(result.getMessage()).contains("模拟模式");
    }

    @Test
    void getModelNameReadsFromSystemConfig() {
        when(systemConfigService.get("ai.llm.model", "deepseek-chat")).thenReturn("deepseek-reasoner");
        assertThat(llmService.getModelName()).isEqualTo("deepseek-reasoner");
    }
}
