package com.aiinterview.controller.admin;

import com.aiinterview.common.Result;
import com.aiinterview.entity.Question;
import com.aiinterview.entity.SystemConfig;
import com.aiinterview.mapper.QuestionMapper;
import com.aiinterview.mapper.SystemConfigMapper;
import com.aiinterview.service.ai.LlmService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "管理员-AI")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminAiController {

    private final SystemConfigMapper configMapper;
    private final QuestionMapper questionMapper;
    private final LlmService llmService;

    @GetMapping("/ai-config")
    public Result<List<Map<String, Object>>> getAiConfig() {
        List<SystemConfig> configs = configMapper.selectList(new LambdaQueryWrapper<SystemConfig>()
                .likeRight(SystemConfig::getConfigKey, "ai."));
        return Result.success(configs.stream().map(this::maskConfig).collect(Collectors.toList()));
    }

    @PutMapping("/ai-config")
    public Result<Void> updateAiConfig(@RequestBody List<ConfigItem> items) {
        for (ConfigItem item : items) {
            SystemConfig cfg = configMapper.findByKey(item.key);
            if (cfg != null) {
                cfg.setConfigValue(item.value);
                configMapper.updateById(cfg);
            }
        }
        return Result.success();
    }

    @GetMapping("/ai-config/test")
    public Result<Map<String, Object>> testLlm() {
        long start = System.currentTimeMillis();
        Map<String, Object> m = new HashMap<>();
        if (llmService.isAvailable()) {
            String reply = llmService.chat(List.of(new LlmService.ChatMessage("user", "ping")));
            m.put("success", StringUtils.hasText(reply));
            m.put("model", llmService.getModelName());
            m.put("latencyMs", System.currentTimeMillis() - start);
            m.put("message", "LLM 服务连接正常");
        } else {
            m.put("success", true);
            m.put("model", "mock");
            m.put("latencyMs", 5);
            m.put("message", "未配置 API Key，使用模拟模式");
        }
        return Result.success(m);
    }

    @PostMapping("/ai/questions/generate")
    public Result<Map<String, Object>> generateQuestions(@RequestBody GenerateRequest req) {
        List<Long> ids = new ArrayList<>();
        int count = req.count != null ? req.count : 1;
        for (int i = 0; i < count; i++) {
            Question q = new Question();
            q.setPositionCode(req.positionCode);
            q.setQuestionType(req.questionType);
            q.setPrimaryKbModuleId(req.kbModuleAnchorId);
            q.setDifficulty(req.difficulty != null ? req.difficulty : 2);
            q.setTopic(req.seedTopic != null ? req.seedTopic : "AI生成");
            q.setSource("AI_TECH_SCENARIO");
            String title = llmService.isAvailable()
                    ? llmService.chat(List.of(new LlmService.ChatMessage("user",
                    "生成一道" + req.questionType + "面试题，主题：" + req.seedTopic)))
                    : "（模拟）请说明 " + req.seedTopic + " 的核心原理与应用场景";
            q.setTitle(title);
            Map<String, Object> meta = new HashMap<>();
            meta.put("kbPointIds", req.kbPointIds);
            meta.put("seedTopic", req.seedTopic);
            q.setGenerationMeta(meta);
            questionMapper.insert(q);
            ids.add(q.getId());
        }
        return Result.success(Map.of("questionIds", ids));
    }

    private Map<String, Object> maskConfig(SystemConfig cfg) {
        Map<String, Object> m = new HashMap<>();
        m.put("key", cfg.getConfigKey());
        String value = cfg.getConfigValue();
        if (cfg.getIsSensitive() != null && cfg.getIsSensitive() == 1 && StringUtils.hasText(value)) {
            value = value.length() <= 4 ? "****" : value.substring(0, 2) + "**********************" + value.substring(value.length() - 2);
        }
        m.put("value", value);
        m.put("type", cfg.getConfigType());
        m.put("sensitive", cfg.getIsSensitive() != null && cfg.getIsSensitive() == 1);
        return m;
    }

    @Data
    public static class ConfigItem {
        private String key;
        private String value;
    }

    @Data
    public static class GenerateRequest {
        private String positionCode;
        private String questionType;
        private Long kbModuleAnchorId;
        private List<Long> kbPointIds;
        private Integer difficulty;
        private Integer count;
        private String seedTopic;
    }
}
