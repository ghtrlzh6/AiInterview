package com.aiinterview.service;

import com.aiinterview.dto.interview.CodeRunRequest;

import java.util.List;
import java.util.Map;

/**
 * 代码沙箱执行服务接口
 * 对接 Piston API 实现多语言代码运行与测试用例判断
 */
public interface CodeExecutionService {

    /**
     * 执行代码并返回结果
     * @param request 运行请求（含代码、语言、模式）
     * @return 包含 stdout/stderr/testResults 的结果 Map
     */
    Map<String, Object> runCode(CodeRunRequest request);

    /**
     * 单次纯代码执行（给定 stdin，返回 stdout/stderr/exitCode）
     * @param language 语言标识
     * @param code     完整代码
     * @param stdin    标准输入
     * @return {stdout, stderr, exitCode, error}
     */
    Map<String, Object> execute(String language, String code, String stdin);

    /**
     * 将语言标识映射到 Piston API 的语言名
     */
    String toPistonLanguage(String language);

    /** 是否配置了可用的执行引擎 */
    boolean isAvailable();

    /**
     * 测试用例结果
     */
    record TestCaseResult(
        int index,
        String description,
        String input,
        String expected,
        String actual,
        boolean passed,
        String error
    ) {}
}
