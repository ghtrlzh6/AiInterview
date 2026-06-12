package com.aiinterview.service.impl;

import com.aiinterview.entity.SystemConfig;
import com.aiinterview.mapper.SystemConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemConfigServiceImplTest {

    @Mock
    private SystemConfigMapper configMapper;

    @InjectMocks
    private SystemConfigServiceImpl systemConfigService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(systemConfigService, "envApiKey", "");
    }

    @Test
    void reloadLoadsConfigsIntoCache() {
        SystemConfig temperature = config("ai.llm.temperature", "0.7");
        SystemConfig apiKey = config("ai.llm.api-key", "sk-test");
        when(configMapper.selectList(any())).thenReturn(List.of(temperature, apiKey));

        systemConfigService.reload();

        assertThat(systemConfigService.get("ai.llm.temperature")).isEqualTo("0.7");
        assertThat(systemConfigService.get("ai.llm.api-key")).isEqualTo("sk-test");
    }

    @Test
    void setUpdatesDatabaseAndCache() {
        SystemConfig cfg = config("ai.llm.temperature", "0.7");
        when(configMapper.selectList(any())).thenReturn(List.of(cfg));
        when(configMapper.findByKey("ai.llm.temperature")).thenReturn(cfg);
        systemConfigService.reload();

        systemConfigService.set("ai.llm.temperature", "0.9");

        assertThat(systemConfigService.get("ai.llm.temperature")).isEqualTo("0.9");
        ArgumentCaptor<SystemConfig> captor = ArgumentCaptor.forClass(SystemConfig.class);
        verify(configMapper).updateById(captor.capture());
        assertThat(captor.getValue().getConfigValue()).isEqualTo("0.9");
    }

    @Test
    void setSkipsMaskedSensitiveValue() {
        SystemConfig cfg = config("ai.llm.api-key", "sk-real-key");
        when(configMapper.selectList(any())).thenReturn(List.of(cfg));
        systemConfigService.reload();

        systemConfigService.set("ai.llm.api-key", "sk-**********************ey");

        assertThat(systemConfigService.get("ai.llm.api-key")).isEqualTo("sk-real-key");
        verify(configMapper, never()).updateById(any(SystemConfig.class));
    }

    @Test
    void setBatchAppliesMultipleUpdates() {
        SystemConfig temperature = config("ai.llm.temperature", "0.7");
        SystemConfig maxTokens = config("ai.llm.max-tokens", "4096");
        when(configMapper.selectList(any())).thenReturn(List.of(temperature, maxTokens));
        when(configMapper.findByKey("ai.llm.temperature")).thenReturn(temperature);
        when(configMapper.findByKey("ai.llm.max-tokens")).thenReturn(maxTokens);
        systemConfigService.reload();

        systemConfigService.setBatch(java.util.Map.of(
                "ai.llm.temperature", "0.5",
                "ai.llm.max-tokens", "2048"
        ));

        assertThat(systemConfigService.get("ai.llm.temperature")).isEqualTo("0.5");
        assertThat(systemConfigService.get("ai.llm.max-tokens")).isEqualTo("2048");
        verify(configMapper, times(2)).updateById(any(SystemConfig.class));
    }

    private SystemConfig config(String key, String value) {
        SystemConfig cfg = new SystemConfig();
        cfg.setId(1L);
        cfg.setConfigKey(key);
        cfg.setConfigValue(value);
        return cfg;
    }
}
