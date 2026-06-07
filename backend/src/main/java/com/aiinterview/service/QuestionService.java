package com.aiinterview.service;

import com.aiinterview.common.PageResult;
import com.aiinterview.entity.QuestionKbPoint;

import java.util.List;
import java.util.Map;

public interface QuestionService {

    PageResult<Map<String, Object>> list(String positionCode, String questionType, Integer difficulty,
                                         Long kbModuleId, int page, int size);

    /**
     * 根据岗位获取知识库节点列表
     */
    List<Map<String, Object>> getKbNodesByPosition(String positionCode);

    /**
     * 批量关联题目与知识库节点
     * @param questionId 题目ID
     * @param kbNodeIds 知识库节点ID列表
     * @return 关联结果
     */
    boolean bindQuestionKbPoints(Long questionId, List<Long> kbNodeIds);

    /**
     * 获取题目的知识库关联列表
     * @param questionId 题目ID
     * @return 关联的知识库节点列表
     */
    List<Map<String, Object>> getQuestionKbPoints(Long questionId);

    /**
     * 批量导入题目时自动关联知识库（基于topic匹配）
     * @param positionCode 岗位代码
     * @return 自动关联的题目数量
     */
    int autoBindQuestionsByTopic(String positionCode);
}
