package com.aiinterview.dto.user;

import lombok.Data;

@Data
public class UpdateProfileRequest {

    private String nickname;
    private String school;
    private String major;
    private String targetPositionCode;
    private String avatarUrl;
    private String email;
}
