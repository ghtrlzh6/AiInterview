package com.aiinterview.entity;

import com.aiinterview.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_kb_article", autoResultMap = true)
public class KbArticle extends BaseEntity {

    private Long kbNodeId;
    private String title;
    private String bodyMarkdown;
    private Integer displayOrder;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> chromaIds;

    private Integer isVectorized;
}
