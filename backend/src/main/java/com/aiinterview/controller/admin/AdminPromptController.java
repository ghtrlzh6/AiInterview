package com.aiinterview.controller.admin;

import com.aiinterview.common.BusinessException;
import com.aiinterview.common.Result;
import com.aiinterview.entity.SystemConfig;
import com.aiinterview.mapper.SystemConfigMapper;
import com.aiinterview.util.PromptTemplateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "管理员-Prompt")
@RestController
@RequestMapping("/api/v1/admin/prompts")
@RequiredArgsConstructor
public class AdminPromptController {

    private final SystemConfigMapper configMapper;
    private final ResourceLoader resourceLoader;

    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        List<SystemConfig> configs = configMapper.selectList(new LambdaQueryWrapper<SystemConfig>()
                .likeRight(SystemConfig::getConfigKey, "prompt."));
        return Result.success(configs.stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("key", c.getConfigKey());
            m.put("description", c.getDescription());
            return m;
        }).collect(Collectors.toList()));
    }

    @GetMapping("/{key}")
    public Result<Map<String, Object>> get(@PathVariable String key) {
        SystemConfig cfg = findPrompt(key);
        Map<String, Object> m = new HashMap<>();
        m.put("key", cfg.getConfigKey());
        m.put("value", cfg.getConfigValue());
        m.put("description", cfg.getDescription());
        return Result.success(m);
    }

    @PutMapping("/{key}")
    public Result<Void> update(@PathVariable String key, @RequestBody PromptUpdateRequest req) {
        SystemConfig cfg = findPrompt(key);
        cfg.setConfigValue(req.value);
        if (req.description != null) cfg.setDescription(req.description);
        configMapper.updateById(cfg);
        return Result.success();
    }

    @PostMapping("/{key}/preview")
    public Result<Map<String, Object>> preview(@PathVariable String key, @RequestBody PreviewRequest req) {
        SystemConfig cfg = findPrompt(key);
        String rendered = PromptTemplateUtil.render(cfg.getConfigValue(), req.variables);
        return Result.success(Map.of("rendered", rendered));
    }

    private SystemConfig findPrompt(String key) {
        String fullKey = key.startsWith("prompt.") ? key : "prompt." + key;
        SystemConfig cfg = configMapper.findByKey(fullKey);
        if (cfg == null) {
            cfg = loadFromResource(fullKey);
        }
        if (cfg == null) throw BusinessException.notFound("Prompt 不存在");
        return cfg;
    }

    private SystemConfig loadFromResource(String key) {
        String fileName = key.replace("prompt.", "") + ".txt";
        Resource resource = resourceLoader.getResource("classpath:prompts/" + fileName);
        if (!resource.exists()) return null;
        try {
            SystemConfig cfg = new SystemConfig();
            cfg.setConfigKey(key);
            cfg.setConfigValue(resource.getContentAsString(StandardCharsets.UTF_8));
            cfg.setConfigType("TEXT");
            cfg.setDescription("来自 resources/prompts/" + fileName);
            return cfg;
        } catch (IOException e) {
            return null;
        }
    }

    @Data
    public static class PromptUpdateRequest {
        private String value;
        private String description;
    }

    @Data
    public static class PreviewRequest {
        private Map<String, String> variables;
    }
}
