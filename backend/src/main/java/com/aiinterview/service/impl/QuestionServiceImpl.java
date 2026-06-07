package com.aiinterview.service.impl;

import com.aiinterview.common.PageResult;
import com.aiinterview.entity.KbNode;
import com.aiinterview.entity.Question;
import com.aiinterview.entity.QuestionKbPoint;
import com.aiinterview.mapper.KbNodeMapper;
import com.aiinterview.mapper.QuestionKbPointMapper;
import com.aiinterview.mapper.QuestionMapper;
import com.aiinterview.service.QuestionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final QuestionMapper questionMapper;
    private final KbNodeMapper kbNodeMapper;
    private final QuestionKbPointMapper questionKbPointMapper;

    @Override
    public PageResult<Map<String, Object>> list(String positionCode, String questionType, Integer difficulty,
                                                Long kbModuleId, int page, int size) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
                .eq(Question::getPositionCode, positionCode)
                .isNull(Question::getBindingSessionId)
                .orderByAsc(Question::getSortOrder);
        if (StringUtils.hasText(questionType)) {
            wrapper.eq(Question::getQuestionType, questionType);
        }
        if (difficulty != null) {
            wrapper.eq(Question::getDifficulty, difficulty);
        }
        if (kbModuleId != null) {
            wrapper.eq(Question::getPrimaryKbModuleId, kbModuleId);
        }
        Page<Question> p = questionMapper.selectPage(new Page<>(page, size), wrapper);
        List<Map<String, Object>> list = p.getRecords().stream().map(q -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", q.getId());
            m.put("positionCode", q.getPositionCode());
            m.put("title", q.getTitle());
            m.put("difficulty", q.getDifficulty());
            m.put("questionType", q.getQuestionType());
            m.put("topic", q.getTopic());
            m.put("primaryKbModuleId", q.getPrimaryKbModuleId());
            if (q.getPrimaryKbModuleId() != null) {
                KbNode node = kbNodeMapper.selectById(q.getPrimaryKbModuleId());
                m.put("primaryKbModuleTitle", node != null ? node.getTitle() : "");
            }
            return m;
        }).collect(Collectors.toList());
        return new PageResult<>(p.getTotal(), page, size, list);
    }

    @Override
    public List<Map<String, Object>> getKbNodesByPosition(String positionCode) {
        // 查询该岗位相关的知识库节点（通过position_codes字段匹配）
        LambdaQueryWrapper<KbNode> wrapper = new LambdaQueryWrapper<KbNode>()
                .eq(KbNode::getNodeType, "TOPIC_POINT")
                .like(KbNode::getPositionCodes, positionCode)
                .orderByAsc(KbNode::getSortOrder);

        List<KbNode> nodes = kbNodeMapper.selectList(wrapper);
        return nodes.stream().map(node -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", node.getId());
            m.put("title", node.getTitle());
            m.put("slug", node.getSlug());
            m.put("codePath", node.getCodePath());
            m.put("summaryExcerpt", node.getSummaryExcerpt());
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean bindQuestionKbPoints(Long questionId, List<Long> kbNodeIds) {
        // 删除旧的关联
        LambdaQueryWrapper<QuestionKbPoint> wrapper = new LambdaQueryWrapper<QuestionKbPoint>()
                .eq(QuestionKbPoint::getQuestionId, questionId);
        questionKbPointMapper.delete(wrapper);

        // 插入新的关联
        if (kbNodeIds != null && !kbNodeIds.isEmpty()) {
            for (Long kbNodeId : kbNodeIds) {
                QuestionKbPoint point = new QuestionKbPoint();
                point.setQuestionId(questionId);
                point.setKbNodeId(kbNodeId);
                questionKbPointMapper.insert(point);
            }
        }

        // 同时更新题目表的主知识库节点
        Question question = questionMapper.selectById(questionId);
        if (question != null && kbNodeIds != null && !kbNodeIds.isEmpty()) {
            question.setPrimaryKbModuleId(kbNodeIds.get(0));
            questionMapper.updateById(question);
        }

        return true;
    }

    @Override
    public List<Map<String, Object>> getQuestionKbPoints(Long questionId) {
        LambdaQueryWrapper<QuestionKbPoint> wrapper = new LambdaQueryWrapper<QuestionKbPoint>()
                .eq(QuestionKbPoint::getQuestionId, questionId);
        List<QuestionKbPoint> points = questionKbPointMapper.selectList(wrapper);

        List<Long> kbNodeIds = points.stream()
                .map(QuestionKbPoint::getKbNodeId)
                .collect(Collectors.toList());

        if (kbNodeIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<KbNode> nodes = kbNodeMapper.selectBatchIds(kbNodeIds);
        return nodes.stream().map(node -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", node.getId());
            m.put("title", node.getTitle());
            m.put("slug", node.getSlug());
            m.put("codePath", node.getCodePath());
            // 查找对应的权重
            QuestionKbPoint point = points.stream()
                    .filter(p -> p.getKbNodeId().equals(node.getId()))
                    .findFirst().orElse(null);
            m.put("relevanceWeight", point != null ? point.getRelevanceWeight() : 1.0);
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public int autoBindQuestionsByTopic(String positionCode) {
        int bindCount = 0;

        // 获取该岗位的所有题目
        LambdaQueryWrapper<Question> qWrapper = new LambdaQueryWrapper<Question>()
                .eq(Question::getPositionCode, positionCode);
        List<Question> questions = questionMapper.selectList(qWrapper);

        // 获取该岗位的所有知识库节点
        List<KbNode> kbNodes = kbNodeMapper.selectList(
                new LambdaQueryWrapper<KbNode>()
                        .like(KbNode::getPositionCodes, positionCode)
                        .eq(KbNode::getNodeType, "TOPIC_POINT")
        );

        // 创建topic到kbNode的映射
        Map<String, KbNode> topicToKbNode = new HashMap<>();
        for (KbNode node : kbNodes) {
            // 简单匹配：kbNode的slug与topic关键词匹配
            topicToKbNode.put(node.getTitle().toLowerCase(), node);
            topicToKbNode.put(node.getSlug().toLowerCase(), node);
        }

        for (Question question : questions) {
            String topic = question.getTopic();
            if (!StringUtils.hasText(topic)) {
                continue;
            }

            // 尝试匹配
            KbNode matchedNode = null;
            String topicLower = topic.toLowerCase();

            // 完全匹配
            if (topicToKbNode.containsKey(topicLower)) {
                matchedNode = topicToKbNode.get(topicLower);
            } else {
                // 部分匹配
                for (Map.Entry<String, KbNode> entry : topicToKbNode.entrySet()) {
                    if (topicLower.contains(entry.getKey()) || entry.getKey().contains(topicLower)) {
                        matchedNode = entry.getValue();
                        break;
                    }
                }
            }

            if (matchedNode != null) {
                // 关联
                QuestionKbPoint point = new QuestionKbPoint();
                point.setQuestionId(question.getId());
                point.setKbNodeId(matchedNode.getId());
                point.setRelevanceWeight(BigDecimal.valueOf(1.0));

                // 检查是否已存在
                LambdaQueryWrapper<QuestionKbPoint> existWrapper = new LambdaQueryWrapper<QuestionKbPoint>()
                        .eq(QuestionKbPoint::getQuestionId, question.getId())
                        .eq(QuestionKbPoint::getKbNodeId, matchedNode.getId());
                long count = questionKbPointMapper.selectCount(existWrapper);

                if (count == 0) {
                    questionKbPointMapper.insert(point);
                    bindCount++;
                }

                // 更新主知识库节点
                if (question.getPrimaryKbModuleId() == null) {
                    question.setPrimaryKbModuleId(matchedNode.getId());
                    questionMapper.updateById(question);
                }
            }
        }

        log.info("岗位 {} 自动关联了 {} 道题目", positionCode, bindCount);
        return bindCount;
    }
}
