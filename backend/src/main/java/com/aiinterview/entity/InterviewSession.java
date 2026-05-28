package com.aiinterview.entity;

import com.aiinterview.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_interview_session")
public class InterviewSession extends BaseEntity {

    private Long userId;
    private Long resumeSnapshotId;
    private String positionCode;
    private String sessionStatus;
    private String inputMode;
    private Integer totalQuestions;
    private Integer answeredCount;
    private Integer durationSeconds;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
