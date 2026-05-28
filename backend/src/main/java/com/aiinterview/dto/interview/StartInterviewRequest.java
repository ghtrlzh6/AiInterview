package com.aiinterview.dto.interview;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StartInterviewRequest {

    @NotBlank
    private String positionCode;

    private String inputMode = "TEXT";

    @Min(3)
    @Max(15)
    private Integer questionCount = 8;

    private Long resumeSnapshotId;
}
