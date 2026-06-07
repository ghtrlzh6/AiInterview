package com.aiinterview.entity;

import com.aiinterview.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_user")
public class User extends BaseEntity {

    private String username;
    private String password;
    private String nickname;
    private String avatarUrl;
    private String email;
    private String school;
    private String major;
    private String educationExperience;
    private String personalSkills;
    private String projectExperience;
    private String internshipExperience;
    private String role;
    private String targetPositionCode;
    private Integer totalInterviews;
}
