package com.aiinterview.controller.admin;

import com.aiinterview.common.BusinessException;
import com.aiinterview.common.Result;
import com.aiinterview.entity.Position;
import com.aiinterview.mapper.PositionMapper;
import com.aiinterview.service.PositionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "管理员-岗位")
@RestController
@RequestMapping("/api/v1/admin/positions")
@RequiredArgsConstructor
public class AdminPositionController {

    private final PositionMapper positionMapper;
    private final PositionService positionService;

    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        return Result.success(positionService.listAllIncludingInactive().stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("code", p.getCode());
            m.put("name", p.getName());
            m.put("description", p.getDescription());
            m.put("techStack", p.getTechStack());
            m.put("sortOrder", p.getSortOrder());
            m.put("isActive", p.getIsActive());
            return m;
        }).collect(Collectors.toList()));
    }

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody PositionRequest req) {
        validateRequired(req);
        String code = req.code.trim();
        Position existing = positionMapper.selectAnyByCode(code);
        if (existing != null) {
            if (existing.getIsDeleted() == null || existing.getIsDeleted() == 0) {
                throw new BusinessException("岗位编码已存在");
            }
            Position restored = toEntity(req, existing);
            restored.setCode(code);
            positionMapper.restoreDeleted(restored);
            return Result.success(Map.of("id", restored.getId()));
        }
        Position p = toEntity(req, new Position());
        positionMapper.insert(p);
        return Result.success(Map.of("id", p.getId()));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody PositionRequest req) {
        Position p = positionMapper.selectById(id);
        if (p == null) throw BusinessException.notFound("岗位不存在");
        toEntity(req, p);
        positionMapper.updateById(p);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> status(@PathVariable Long id, @RequestBody StatusRequest req) {
        Position p = positionMapper.selectById(id);
        if (p == null) throw BusinessException.notFound("岗位不存在");
        p.setIsActive(req.isActive ? 1 : 0);
        positionMapper.updateById(p);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        positionMapper.deleteById(id);
        return Result.success();
    }

    private Position toEntity(PositionRequest req, Position p) {
        if (StringUtils.hasText(req.code)) p.setCode(req.code.trim());
        if (StringUtils.hasText(req.name)) p.setName(req.name.trim());
        if (req.description != null) p.setDescription(req.description);
        if (req.techStack != null) p.setTechStack(req.techStack);
        if (req.sortOrder != null) p.setSortOrder(req.sortOrder);
        if (req.iconUrl != null) p.setIconUrl(req.iconUrl);
        if (p.getIsActive() == null) p.setIsActive(1);
        return p;
    }

    private void validateRequired(PositionRequest req) {
        if (req == null || !StringUtils.hasText(req.code)) {
            throw new BusinessException("请填写岗位编码");
        }
        if (!StringUtils.hasText(req.name)) {
            throw new BusinessException("请填写岗位名称");
        }
    }

    @Data
    public static class PositionRequest {
        private String code;
        private String name;
        private String description;
        private List<String> techStack;
        private Integer sortOrder;
        private String iconUrl;
    }

    @Data
    public static class StatusRequest {
        private boolean isActive;
    }
}
