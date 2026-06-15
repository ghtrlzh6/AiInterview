package com.aiinterview.entity;

import com.aiinterview.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_session_coding_submit")
public class SessionCodingSubmit extends BaseEntity {

    private Long sessionId;
    private Long questionId;
    private String codeBody;
    private String language;
    private Integer submitOrder;

    /** 运行状态：PASSED / FAILED / ERROR / TIMEOUT / PENDING */
    private String runStatus;
    /** 通过的测试用例数 */
    private Integer testsPassed;
    /** 总测试用例数 */
    private Integer testsTotal;
    /** 标准输出 */
    private String runStdout;
    /** 标准错误 */
    private String runStderr;
}
