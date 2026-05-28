package com.aiinterview.entity;

import com.aiinterview.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_user_recommendation")
public class UserRecommendation extends BaseEntity {

    private Long userId;
    private Long reportId;
    private Long resourceId;
    private String reason;
    private Integer isClicked;
    private Integer isHelpful;
}
