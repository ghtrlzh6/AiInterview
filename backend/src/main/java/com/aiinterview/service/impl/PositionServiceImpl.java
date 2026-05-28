package com.aiinterview.service.impl;

import com.aiinterview.common.BusinessException;
import com.aiinterview.entity.Position;
import com.aiinterview.mapper.PositionMapper;
import com.aiinterview.service.PositionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PositionServiceImpl implements PositionService {

    private final PositionMapper positionMapper;

    @Override
    public List<Map<String, Object>> listActive() {
        return positionMapper.selectList(new LambdaQueryWrapper<Position>()
                        .eq(Position::getIsActive, 1)
                        .orderByAsc(Position::getSortOrder))
                .stream().map(this::toMap).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getByCode(String code) {
        Position p = positionMapper.selectOne(new LambdaQueryWrapper<Position>().eq(Position::getCode, code));
        if (p == null) {
            throw BusinessException.notFound("岗位不存在");
        }
        return toMap(p);
    }

    @Override
    public List<Position> listAllIncludingInactive() {
        return positionMapper.selectList(new LambdaQueryWrapper<Position>().orderByAsc(Position::getSortOrder));
    }

    private Map<String, Object> toMap(Position p) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", p.getId());
        m.put("code", p.getCode());
        m.put("name", p.getName());
        m.put("description", p.getDescription());
        m.put("techStack", p.getTechStack());
        m.put("iconUrl", p.getIconUrl());
        m.put("sortOrder", p.getSortOrder());
        m.put("isActive", p.getIsActive());
        return m;
    }
}
