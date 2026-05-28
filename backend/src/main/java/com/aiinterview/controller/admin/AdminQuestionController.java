package com.aiinterview.controller.admin;

import com.aiinterview.common.BusinessException;
import com.aiinterview.common.PageResult;
import com.aiinterview.common.Result;
import com.aiinterview.entity.Question;
import com.aiinterview.entity.QuestionKbPoint;
import com.aiinterview.mapper.QuestionKbPointMapper;
import com.aiinterview.mapper.QuestionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "管理员-题目")
@RestController
@RequestMapping("/api/v1/admin/questions")
@RequiredArgsConstructor
public class AdminQuestionController {

    private final QuestionMapper questionMapper;
    private final QuestionKbPointMapper kbPointMapper;

    @GetMapping
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(required = false) String positionCode,
            @RequestParam(required = false) String questionType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        LambdaQueryWrapper<Question> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(positionCode)) w.eq(Question::getPositionCode, positionCode);
        if (StringUtils.hasText(questionType)) w.eq(Question::getQuestionType, questionType);
        Page<Question> p = questionMapper.selectPage(new Page<>(page, size), w);
        List<Map<String, Object>> list = p.getRecords().stream().map(this::toMap).collect(Collectors.toList());
        return Result.success(new PageResult<>(p.getTotal(), page, size, list));
    }

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody QuestionRequest req) {
        Question q = fromRequest(req, new Question());
        q.setSource("MANUAL");
        questionMapper.insert(q);
        saveKbPoints(q.getId(), req.kbPointIds);
        return Result.success(Map.of("id", q.getId()));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody QuestionRequest req) {
        Question q = questionMapper.selectById(id);
        if (q == null) throw BusinessException.notFound("题目不存在");
        fromRequest(req, q);
        questionMapper.updateById(q);
        if (req.kbPointIds != null) {
            kbPointMapper.delete(new LambdaQueryWrapper<QuestionKbPoint>().eq(QuestionKbPoint::getQuestionId, id));
            saveKbPoints(id, req.kbPointIds);
        }
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        questionMapper.deleteById(id);
        return Result.success();
    }

    @PostMapping("/batch-import")
    public Result<Map<String, Object>> batchImport(@RequestBody List<QuestionRequest> items) {
        int count = 0;
        for (QuestionRequest req : items) {
            Question q = fromRequest(req, new Question());
            q.setSource("BATCH_IMPORT");
            questionMapper.insert(q);
            saveKbPoints(q.getId(), req.kbPointIds);
            count++;
        }
        return Result.success(Map.of("imported", count));
    }

    private void saveKbPoints(Long questionId, List<Long> kbPointIds) {
        if (kbPointIds == null) return;
        for (Long kbId : kbPointIds) {
            QuestionKbPoint qkp = new QuestionKbPoint();
            qkp.setQuestionId(questionId);
            qkp.setKbNodeId(kbId);
            kbPointMapper.insert(qkp);
        }
    }

    private Question fromRequest(QuestionRequest req, Question q) {
        if (req.positionCode != null) q.setPositionCode(req.positionCode);
        if (req.primaryKbModuleId != null) q.setPrimaryKbModuleId(req.primaryKbModuleId);
        if (req.codingChallengeId != null) q.setCodingChallengeId(req.codingChallengeId);
        if (req.title != null) q.setTitle(req.title);
        if (req.answerReference != null) q.setAnswerReference(req.answerReference);
        if (req.difficulty != null) q.setDifficulty(req.difficulty);
        if (req.questionType != null) q.setQuestionType(req.questionType);
        if (req.topic != null) q.setTopic(req.topic);
        if (req.followUpHints != null) q.setFollowUpHints(req.followUpHints);
        return q;
    }

    private Map<String, Object> toMap(Question q) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", q.getId());
        m.put("positionCode", q.getPositionCode());
        m.put("title", q.getTitle());
        m.put("difficulty", q.getDifficulty());
        m.put("questionType", q.getQuestionType());
        m.put("topic", q.getTopic());
        m.put("answerReference", q.getAnswerReference());
        m.put("followUpHints", q.getFollowUpHints());
        return m;
    }

    @Data
    public static class QuestionRequest {
        private String positionCode;
        private Long primaryKbModuleId;
        private List<Long> kbPointIds;
        private Long codingChallengeId;
        private String title;
        private String answerReference;
        private Integer difficulty;
        private String questionType;
        private String topic;
        private List<String> followUpHints;
    }
}
