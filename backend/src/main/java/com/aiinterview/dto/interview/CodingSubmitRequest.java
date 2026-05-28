package com.aiinterview.dto.interview;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CodingSubmitRequest {

    @NotNull
    private Long questionId;

    @NotBlank
    private String language;

    @NotBlank
    private String code;
}
