package com.aiinterview.controller;

import com.aiinterview.common.BusinessException;
import com.aiinterview.common.Result;
import com.aiinterview.dto.interview.CodeRunRequest;
import com.aiinterview.entity.CodingChallenge;
import com.aiinterview.entity.SessionCodingSubmit;
import com.aiinterview.mapper.CodingChallengeMapper;
import com.aiinterview.mapper.InterviewQuestionMapper;
import com.aiinterview.mapper.SessionCodingSubmitMapper;
import com.aiinterview.service.CodeExecutionService;
import com.aiinterview.util.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 代码执行控制器
 * POST /api/v1/coding/run   — 调试运行（run 模式）或正式提交（submit 模式）
 * GET  /api/v1/coding/{id}  — 获取题目详情（含测试用例和起始代码）
 */
@Slf4j
@Tag(name = "代码执行")
@RestController
@RequestMapping("/api/v1/coding")
@RequiredArgsConstructor
public class CodeExecutionController {

    private final CodeExecutionService codeExecutionService;
    private final CodingChallengeMapper challengeMapper;
    private final SessionCodingSubmitMapper submitMapper;
    private final InterviewQuestionMapper interviewQuestionMapper;

    @Operation(summary = "运行/提交代码")
    @PostMapping("/run")
    public Result<Map<String, Object>> runCode(@Valid @RequestBody CodeRunRequest request) {
        Long userId = SecurityUtils.currentUserId();

        Map<String, Object> execResult = codeExecutionService.runCode(request);

        // submit 模式：保存提交记录
        if ("submit".equalsIgnoreCase(request.getMode())
                && request.getSessionId() != null
                && request.getQuestionId() != null) {
            try {
                saveSubmitRecord(userId, request, execResult);
            } catch (Exception e) {
                log.warn("Failed to save coding submit record", e);
            }
        }

        return Result.success(execResult);
    }

    @Operation(summary = "获取编程题详情（含测试用例和起始代码）")
    @GetMapping("/{challengeId}")
    public Result<Map<String, Object>> getChallengeDetail(@PathVariable Long challengeId) {
        CodingChallenge challenge = challengeMapper.selectById(challengeId);
        if (challenge == null) {
            throw BusinessException.notFound("编程题不存在");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", challenge.getId());
        data.put("externalRef", challenge.getExternalRef());
        data.put("title", challenge.getTitle());
        data.put("problemMd", challenge.getProblemMd());
        data.put("difficulty", challenge.getDifficulty());
        data.put("tags", challenge.getCanonicalTags());
        data.put("judgeConfig", challenge.getJudgeConfig());
        data.put("starterCode", challenge.getStarterCode());
        data.put("answerHintMd", challenge.getAnswerHintMd());
        return Result.success(data);
    }

    private void saveSubmitRecord(Long userId, CodeRunRequest request, Map<String, Object> execResult) {
        Long count = submitMapper.selectCount(new LambdaQueryWrapper<SessionCodingSubmit>()
                .eq(SessionCodingSubmit::getSessionId, request.getSessionId())
                .eq(SessionCodingSubmit::getQuestionId, request.getQuestionId()));

        SessionCodingSubmit submit = new SessionCodingSubmit();
        submit.setSessionId(request.getSessionId());
        submit.setQuestionId(request.getQuestionId());
        submit.setCodeBody(request.getCode());
        submit.setLanguage(request.getLanguage());
        submit.setSubmitOrder(count.intValue() + 1);

        String runStatus = String.valueOf(execResult.getOrDefault("runStatus", "PENDING"));
        submit.setRunStatus(runStatus);

        Object passedObj = execResult.get("passed");
        Object totalObj  = execResult.get("total");
        if (passedObj != null) submit.setTestsPassed(((Number) passedObj).intValue());
        if (totalObj  != null) submit.setTestsTotal(((Number) totalObj).intValue());

        String stdout = String.valueOf(execResult.getOrDefault("stdout", ""));
        String stderr = String.valueOf(execResult.getOrDefault("stderr", ""));
        if (stdout.length() > 65535) stdout = stdout.substring(0, 65535);
        if (stderr.length() > 8191)  stderr = stderr.substring(0, 8191);
        submit.setRunStdout(stdout);
        submit.setRunStderr(stderr);

        submitMapper.insert(submit);
    }
}
