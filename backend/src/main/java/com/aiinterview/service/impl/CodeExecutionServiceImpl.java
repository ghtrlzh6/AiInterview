package com.aiinterview.service.impl;

import cn.hutool.json.JSONUtil;
import com.aiinterview.dto.interview.CodeRunRequest;
import com.aiinterview.entity.CodingChallenge;
import com.aiinterview.mapper.CodingChallengeMapper;
import com.aiinterview.service.CodeExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 代码执行服务实现
 * 对接 Piston API 运行多语言代码。
 *
 * 公共实例 https://emkc.org 自 2026-02-15 起需 Authorization Token（401 即未配置 Key）。
 * 课设/生产环境推荐在同一台服务器自建 Piston Docker，设置 PISTON_API_URL 指向本地，无需 Key。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeExecutionServiceImpl implements CodeExecutionService {

    @Value("${piston.api-url:https://emkc.org/api/v2/piston/execute}")
    private String pistonApiUrl;

    @Value("${piston.api-key:}")
    private String pistonApiKey;

    private final CodingChallengeMapper challengeMapper;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build();

    @Override
    public boolean isAvailable() {
        if (!StringUtils.hasText(pistonApiUrl)) {
            return false;
        }
        // 公共 emkc 实例必须带 Key；自建实例可不配置 Key
        if (pistonApiUrl.contains("emkc.org") && !StringUtils.hasText(pistonApiKey)) {
            return false;
        }
        return true;
    }

    @Override
    public String toPistonLanguage(String language) {
        return switch (language.toLowerCase()) {
            case "java"       -> "java";
            case "python"     -> "python";
            case "cpp", "c++" -> "c++";
            case "javascript", "typescript" -> "javascript";
            case "csharp", "c#" -> "csharp";
            default -> language.toLowerCase();
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> runCode(CodeRunRequest request) {
        CodingChallenge challenge = challengeMapper.selectById(request.getChallengeId());
        if (challenge == null) {
            return errorResult("题目不存在");
        }

        Map<String, Object> judgeConfig = challenge.getJudgeConfig();
        boolean isRunMode = "run".equalsIgnoreCase(request.getMode());

        // run 模式：只跑第一个测试用例
        if (isRunMode || judgeConfig == null || !judgeConfig.containsKey("testCases")) {
            String sampleInput = getSampleInput(judgeConfig);
            Map<String, Object> execResult = execute(request.getLanguage(), request.getCode(), sampleInput);
            Map<String, Object> result = new HashMap<>(execResult);
            result.put("mode", "run");
            result.put("sampleInput", sampleInput);
            return result;
        }

        // submit 模式：跑全部测试用例
        List<Map<String, Object>> testCases = (List<Map<String, Object>>) judgeConfig.get("testCases");
        if (testCases == null || testCases.isEmpty()) {
            Map<String, Object> execResult = execute(request.getLanguage(), request.getCode(), "");
            execResult.put("mode", "submit");
            execResult.put("testResults", Collections.emptyList());
            return execResult;
        }

        List<TestCaseResult> testResults = new ArrayList<>();
        int passed = 0;
        String lastStdout = "";
        String lastStderr = "";

        for (int i = 0; i < testCases.size(); i++) {
            Map<String, Object> tc = testCases.get(i);
            String input    = String.valueOf(tc.getOrDefault("input", ""));
            String expected = String.valueOf(tc.getOrDefault("expected", "")).trim();
            String desc     = String.valueOf(tc.getOrDefault("description", "测试 " + (i + 1)));

            Map<String, Object> execResult = execute(request.getLanguage(), request.getCode(), input);
            String stdout  = String.valueOf(execResult.getOrDefault("stdout", "")).trim();
            String stderr  = String.valueOf(execResult.getOrDefault("stderr", ""));
            String error   = String.valueOf(execResult.getOrDefault("error", ""));
            lastStdout = stdout;
            lastStderr = StringUtils.hasText(stderr) ? stderr : error;

            boolean hasError = StringUtils.hasText(error) || (execResult.containsKey("exitCode") && !Integer.valueOf(0).equals(execResult.get("exitCode")));
            boolean isPassed = !hasError && normalizedMatch(stdout, expected);

            if (isPassed) passed++;
            testResults.add(new TestCaseResult(i + 1, desc, input, expected, stdout, isPassed,
                    hasError ? (StringUtils.hasText(error) ? error : stderr) : null));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("mode", "submit");
        result.put("testResults", testResults);
        result.put("passed", passed);
        result.put("total", testCases.size());
        result.put("allPassed", passed == testCases.size());
        result.put("runStatus", passed == testCases.size() ? "PASSED" : "FAILED");
        result.put("stdout", lastStdout);
        result.put("stderr", lastStderr);
        return result;
    }

    /** 对比输出：逐行 trim 后比较（忽略行尾空白差异） */
    private boolean normalizedMatch(String actual, String expected) {
        if (actual == null || expected == null) return false;
        String[] actualLines   = actual.trim().split("\n");
        String[] expectedLines = expected.trim().split("\n");
        if (actualLines.length != expectedLines.length) return false;
        for (int i = 0; i < actualLines.length; i++) {
            if (!actualLines[i].trim().equals(expectedLines[i].trim())) return false;
        }
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> execute(String language, String code, String stdin) {
        String pistonLang = toPistonLanguage(language);
        String filename = getFilename(language);

        Map<String, Object> file = new LinkedHashMap<>();
        file.put("name", filename);
        file.put("content", code);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("language", pistonLang);
        payload.put("version", "*");
        payload.put("files", List.of(file));
        if (StringUtils.hasText(stdin)) {
            payload.put("stdin", stdin);
        }

        String jsonBody = JSONUtil.toJsonStr(payload);
        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));
        Request.Builder reqBuilder = new Request.Builder()
                .url(pistonApiUrl)
                .post(body)
                .header("Content-Type", "application/json");
        if (StringUtils.hasText(pistonApiKey)) {
            reqBuilder.header("Authorization", pistonApiKey);
        }
        Request req = reqBuilder.build();

        try (Response response = httpClient.newCall(req).execute()) {
            if (!response.isSuccessful()) {
                log.warn("Piston API returned {}: {} url={}", response.code(), response.message(), pistonApiUrl);
                if (response.code() == 401) {
                    return errorResult(
                            "代码执行服务未授权（HTTP 401）。公共 Piston API 自 2026-02-15 起需要 Token，"
                                    + "请在环境变量 PISTON_API_KEY 中配置；或在本机/服务器自建 Piston 并设置 "
                                    + "PISTON_API_URL=http://127.0.0.1:2000/api/v2/execute（自建无需 Key）");
                }
                return errorResult("代码执行服务暂时不可用（HTTP " + response.code() + "），请稍后重试");
            }
            String respBody = response.body() != null ? response.body().string() : "{}";
            Map<String, Object> respMap = JSONUtil.toBean(respBody, Map.class);

            Map<String, Object> run = (Map<String, Object>) respMap.get("run");
            if (run == null) {
                return errorResult("执行引擎返回格式异常");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("stdout",   String.valueOf(run.getOrDefault("stdout", "")));
            result.put("stderr",   String.valueOf(run.getOrDefault("stderr", "")));
            result.put("exitCode", run.get("code"));
            result.put("error",    "");

            // 编译错误通常在 compile 阶段
            Map<String, Object> compile = (Map<String, Object>) respMap.get("compile");
            if (compile != null) {
                String compileStderr = String.valueOf(compile.getOrDefault("stderr", ""));
                if (StringUtils.hasText(compileStderr)) {
                    result.put("stderr", compileStderr);
                    result.put("error", "编译错误");
                    result.put("exitCode", 1);
                }
            }
            return result;
        } catch (IOException e) {
            log.error("Piston API call failed", e);
            return errorResult("网络异常，无法连接代码执行服务：" + e.getMessage());
        }
    }

    private String getFilename(String language) {
        return switch (language.toLowerCase()) {
            case "java"       -> "Main.java";
            case "python"     -> "solution.py";
            case "cpp", "c++" -> "main.cpp";
            case "javascript" -> "solution.js";
            case "typescript" -> "solution.ts";
            case "csharp"     -> "Main.cs";
            default -> "solution.txt";
        };
    }

    @SuppressWarnings("unchecked")
    private String getSampleInput(Map<String, Object> judgeConfig) {
        if (judgeConfig == null) return "";
        Object tc = judgeConfig.get("testCases");
        if (tc instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof Map) {
                Map<Object, Object> fm = (Map<Object, Object>) first;
                Object input = fm.get("input");
                return input != null ? String.valueOf(input) : "";
            }
        }
        return "";
    }

    private Map<String, Object> errorResult(String message) {
        Map<String, Object> r = new HashMap<>();
        r.put("error", message);
        r.put("stdout", "");
        r.put("stderr", "");
        r.put("exitCode", -1);
        return r;
    }
}
