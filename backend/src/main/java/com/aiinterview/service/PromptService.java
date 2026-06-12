package com.aiinterview.service;

import com.aiinterview.entity.SystemConfig;
import com.aiinterview.mapper.SystemConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Prompt 配置服务
 * 负责从数据库读取和缓存岗位差异化 Prompt
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptService {

    private final SystemConfigMapper systemConfigMapper;

    // 缓存的 Prompt 配置
    private final Map<String, String> promptCache = new HashMap<>();

    /**
     * 获取面试官系统 Prompt
     * @param positionCode 岗位代码，如 JAVA_BACKEND, WEB_FRONTEND 等
     * @return 岗位对应的面试官 Prompt，如果不存在则返回默认 Prompt
     */
    public String getInterviewPrompt(String positionCode) {
        if (positionCode == null || positionCode.isEmpty()) {
            return getDefaultInterviewPrompt();
        }

        // 尝试获取岗位专属 Prompt
        String key = "prompt.interview.system." + positionCode.toLowerCase();
        String prompt = getConfigValue(key);

        if (prompt != null && !prompt.isEmpty()) {
            return prompt;
        }

        // 如果没有找到岗位专属 Prompt，返回默认 Prompt
        return getDefaultInterviewPrompt();
    }

    /**
     * 获取默认面试官 Prompt
     */
    public String getDefaultInterviewPrompt() {
        String key = "prompt.interview.system";
        String prompt = getConfigValue(key);

        // 如果数据库也没有，返回内置默认值
        if (prompt == null || prompt.isEmpty()) {
            return getBuiltInDefaultPrompt();
        }

        return prompt;
    }

    /**
     * 获取逐题评估 Prompt
     */
    public String getEvaluationQuestionPrompt() {
        String key = "prompt.evaluation.question";
        String prompt = getConfigValue(key);

        if (prompt == null || prompt.isEmpty()) {
            return getBuiltInEvaluationQuestionPrompt();
        }

        return prompt;
    }

    /**
     * 获取综合报告 Prompt
     */
    public String getEvaluationFinalPrompt() {
        String key = "prompt.evaluation.final";
        String prompt = getConfigValue(key);

        if (prompt == null || prompt.isEmpty()) {
            return getBuiltInEvaluationFinalPrompt();
        }

        return prompt;
    }

    /**
     * 获取配置值（带缓存）
     */
    private String getConfigValue(String key) {
        // 先从缓存获取
        if (promptCache.containsKey(key)) {
            return promptCache.get(key);
        }

        // 从数据库获取
        SystemConfig config = systemConfigMapper.findByKey(key);
        if (config != null) {
            promptCache.put(key, config.getConfigValue());
            return config.getConfigValue();
        }

        return null;
    }

    /**
     * 清除缓存，强制重新加载
     */
    public void clearCache() {
        promptCache.clear();
        log.info("Prompt 缓存已清除");
    }

    /**
     * 内置默认面试官 Prompt
     */
    private String getBuiltInDefaultPrompt() {
        return """
                你是一位专业严肃的技术面试官，正在对候选人进行面试。
                规则：
                1. 如果候选人的回答不完整或存在明显错误，可以追问，但同一题最多追问2次后必须推进下一题
                2. 追问时请直接针对回答的不足点发问
                3. 推进下一题时，自然过渡，不要生硬
                4. 所有回复用JSON格式输出：{"action":"follow_up|next_question|end","reply":"..."}
                """;
    }

    /**
     * 内置逐题评估 Prompt
     */
    private String getBuiltInEvaluationQuestionPrompt() {
        return """
                请对候选人的回答进行评分。
                评分维度：tech_score（技术准确性）、logic_score（逻辑清晰度）、depth_score（回答深度）
                评分范围：0-100分
                请JSON输出：{"tech_score":0-100,"logic_score":0-100,"depth_score":0-100,"comment":"..."}
                """;
    }

    /**
     * 内置综合报告 Prompt
     */
    private String getBuiltInEvaluationFinalPrompt() {
        return """
                根据各题评分汇总，生成综合面试评估报告。
                需要输出的内容：
                1. overall_score：综合得分（0-100）
                2. expression_score：表达能力得分（0-100）
                3. confidence_score：自信程度得分（0-100）
                4. summary：Markdown 格式的总结
                5. highlights：优点列表
                6. weaknesses：不足点列表
                7. suggestions：改进建议列表
                请JSON输出：{"overall_score":0-100,"expression_score":0-100,"confidence_score":0-100,"summary":"...","highlights":[],"weaknesses":[],"suggestions":[]}
                """;
    }

    /**
     * 获取所有岗位的 Prompt 状态（用于管理后台）
     */
    public Map<String, Boolean> getAllPositionPromptStatus() {
        Map<String, Boolean> status = new HashMap<>();

        String[] positions = {"JAVA_BACKEND", "WEB_FRONTEND", "PYTHON_ALGO", "GAME_CLIENT"};

        for (String position : positions) {
            String key = "prompt.interview.system." + position.toLowerCase();
            SystemConfig config = systemConfigMapper.findByKey(key);
            status.put(position, config != null && config.getConfigValue() != null && !config.getConfigValue().isEmpty());
        }

        // 检查默认 Prompt
        SystemConfig defaultConfig = systemConfigMapper.findByKey("prompt.interview.system");
        status.put("DEFAULT", defaultConfig != null && defaultConfig.getConfigValue() != null && !defaultConfig.getConfigValue().isEmpty());

        return status;
    }
}
