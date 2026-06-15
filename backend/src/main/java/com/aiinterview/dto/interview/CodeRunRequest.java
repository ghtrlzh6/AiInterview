package com.aiinterview.dto.interview;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 代码运行请求
 * mode=run   → 仅运行示例输入，展示输出（不保存记录）
 * mode=submit → 跑全部测试用例，保存到 t_session_coding_submit
 */
@Data
public class CodeRunRequest {

    /** 关联的 coding challenge ID，用于取测试用例 */
    @NotNull
    private Long challengeId;

    /** 关联的面试会话 ID（submit 模式必填） */
    private Long sessionId;

    /** 关联的题目 ID（submit 模式必填） */
    private Long questionId;

    /** 编程语言：java / python / cpp / javascript */
    @NotBlank
    private String language;

    /** 用户代码 */
    @NotBlank
    private String code;

    /** 运行模式：run（调试运行） / submit（正式提交） */
    @NotBlank
    private String mode;
}
