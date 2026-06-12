package com.aiinterview.controller.admin;

import com.aiinterview.common.BusinessException;
import com.aiinterview.common.PageResult;
import com.aiinterview.common.Result;
import com.aiinterview.entity.LearningResource;
import com.aiinterview.mapper.LearningResourceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.util.StringUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "管理员-资源")
@RestController
@RequestMapping("/api/v1/admin/resources")
@RequiredArgsConstructor
public class AdminResourceController {

    private final LearningResourceMapper resourceMapper;

    @GetMapping
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String positionCode) {
        LambdaQueryWrapper<LearningResource> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(positionCode)) {
            wrapper.eq(LearningResource::getPositionCode, positionCode);
        }
        Page<LearningResource> p = resourceMapper.selectPage(new Page<>(page, size), wrapper);
        List<Map<String, Object>> list = p.getRecords().stream().map(this::toMap).collect(Collectors.toList());
        return Result.success(new PageResult<>(p.getTotal(), page, size, list));
    }

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody ResourceRequest req) {
        LearningResource r = fromReq(req, new LearningResource());
        resourceMapper.insert(r);
        return Result.success(Map.of("id", r.getId()));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody ResourceRequest req) {
        LearningResource r = resourceMapper.selectById(id);
        if (r == null) throw BusinessException.notFound("资源不存在");
        fromReq(req, r);
        resourceMapper.updateById(r);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        resourceMapper.deleteById(id);
        return Result.success();
    }

    private LearningResource fromReq(ResourceRequest req, LearningResource r) {
        if (req.positionCode != null) r.setPositionCode(req.positionCode);
        if (req.title != null) r.setTitle(req.title);
        if (req.description != null) r.setDescription(req.description);
        if (req.resourceType != null) r.setResourceType(req.resourceType);
        if (req.url != null) r.setUrl(req.url);
        if (req.topic != null) r.setTopic(req.topic);
        if (req.difficulty != null) r.setDifficulty(req.difficulty);
        if (req.qualityScore != null) r.setQualityScore(req.qualityScore);
        return r;
    }

    private Map<String, Object> toMap(LearningResource r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getId());
        m.put("positionCode", r.getPositionCode());
        m.put("title", r.getTitle());
        m.put("description", r.getDescription());
        m.put("resourceType", r.getResourceType());
        m.put("url", r.getUrl());
        m.put("topic", r.getTopic());
        m.put("difficulty", r.getDifficulty());
        m.put("qualityScore", r.getQualityScore());
        return m;
    }

    @Data
    public static class ResourceRequest {
        private String positionCode;
        private String title;
        private String description;
        private String resourceType;
        private String url;
        private String topic;
        private Integer difficulty;
        private Integer qualityScore;
    }
}
