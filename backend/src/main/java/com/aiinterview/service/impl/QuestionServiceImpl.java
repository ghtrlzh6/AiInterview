package com.aiinterview.service.impl;

import com.aiinterview.common.PageResult;
import com.aiinterview.entity.KbNode;
import com.aiinterview.entity.Question;
import com.aiinterview.mapper.KbNodeMapper;
import com.aiinterview.mapper.QuestionMapper;
import com.aiinterview.service.QuestionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final QuestionMapper questionMapper;
    private final KbNodeMapper kbNodeMapper;

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
}
