package com.aiinterview.entity;

import com.aiinterview.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_learning_resource")
public class LearningResource extends BaseEntity {

    private String positionCode;
    private String title;
    private String description;
    private String resourceType;
    private String url;
    private String topic;
    private Integer difficulty;
    private Integer qualityScore;
}
