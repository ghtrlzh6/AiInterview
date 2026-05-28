package com.aiinterview.controller.admin;

import com.aiinterview.common.BusinessException;
import com.aiinterview.common.PageResult;
import com.aiinterview.common.Result;
import com.aiinterview.entity.User;
import com.aiinterview.mapper.UserMapper;
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

@Tag(name = "管理员-用户")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserMapper userMapper;

    @GetMapping
    public Result<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String username) {
        LambdaQueryWrapper<User> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(username)) w.like(User::getUsername, username);
        Page<User> p = userMapper.selectPage(new Page<>(page, size), w);
        List<Map<String, Object>> list = p.getRecords().stream().map(this::toMap).collect(Collectors.toList());
        return Result.success(new PageResult<>(p.getTotal(), page, size, list));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user == null) throw BusinessException.notFound("用户不存在");
        return Result.success(toMap(user));
    }

    @PutMapping("/{id}/role")
    public Result<Void> updateRole(@PathVariable Long id, @RequestBody RoleRequest req) {
        User user = userMapper.selectById(id);
        if (user == null) throw BusinessException.notFound("用户不存在");
        user.setRole(req.role);
        userMapper.updateById(user);
        return Result.success();
    }

    private Map<String, Object> toMap(User u) {
        Map<String, Object> m = new HashMap<>();
        m.put("userId", u.getId());
        m.put("username", u.getUsername());
        m.put("nickname", u.getNickname());
        m.put("email", u.getEmail());
        m.put("role", u.getRole());
        m.put("totalInterviews", u.getTotalInterviews());
        m.put("createdAt", u.getCreatedAt());
        return m;
    }

    @Data
    public static class RoleRequest {
        private String role;
    }
}
