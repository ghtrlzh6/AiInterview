package com.aiinterview.entity;

import com.aiinterview.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_user_resume")
public class UserResume extends BaseEntity {

    private Long userId;
    private String fileUrl;
    private String fileName;
    private String parseStatus;
    private String resumeTextMd;
    private String remark;
}
