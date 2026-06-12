package com.aiinterview.service.impl;

import com.aiinterview.entity.SystemConfig;
import com.aiinterview.mapper.SystemConfigMapper;
import com.aiinterview.service.SystemConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {

    private final SystemConfigMapper configMapper;
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    @Value("${ai.llm.api-key:}")
    private String envApiKey;

    @PostConstruct
    public void init() {
        reload();
        if (StringUtils.hasText(envApiKey) && !StringUtils.hasText(get("ai.llm.api-key"))) {
            set("ai.llm.api-key", envApiKey);
            log.info("Seeded ai.llm.api-key from environment variable");
        }
    }

    @Override
    public void reload() {
        cache.clear();
        List<SystemConfig> configs = configMapper.selectList(new LambdaQueryWrapper<>());
        for (SystemConfig cfg : configs) {
            if (StringUtils.hasText(cfg.getConfigKey())) {
                cache.put(cfg.getConfigKey(), cfg.getConfigValue() != null ? cfg.getConfigValue() : "");
            }
        }
        log.info("System config cache loaded: {} entries", cache.size());
    }

    @Override
    public String get(String key) {
        return cache.getOrDefault(key, "");
    }

    @Override
    public String get(String key, String defaultValue) {
        String value = cache.get(key);
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    @Override
    public void set(String key, String value) {
        if (!StringUtils.hasText(key) || value == null || isMaskedValue(value)) {
            return;
        }
        SystemConfig cfg = configMapper.findByKey(key);
        if (cfg == null) {
            log.warn("Skip unknown config key: {}", key);
            return;
        }
        cfg.setConfigValue(value);
        configMapper.updateById(cfg);
        cache.put(key, value);
    }

    @Override
    public void setBatch(Map<String, String> updates) {
        if (updates == null || updates.isEmpty()) {
            return;
        }
        updates.forEach(this::set);
    }

    @Override
    public List<String> keysByPrefix(String prefix) {
        return cache.keySet().stream()
                .filter(key -> key.startsWith(prefix))
                .sorted()
                .toList();
    }

    @Override
    public boolean isMaskedValue(String value) {
        return StringUtils.hasText(value) && value.contains("****");
    }
}
