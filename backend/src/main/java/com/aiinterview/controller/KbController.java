package com.aiinterview.controller;

import com.aiinterview.common.Result;
import com.aiinterview.service.KbService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "知识库")
@RestController
@RequestMapping("/api/v1/kb")
@RequiredArgsConstructor
public class KbController {

    private final KbService kbService;

    @Operation(summary = "类目子树")
    @GetMapping("/tree")
    public Result<List<Map<String, Object>>> tree(
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false) String positionCode) {
        return Result.success(kbService.getTree(parentId, positionCode));
    }

    @Operation(summary = "节点详情")
    @GetMapping("/nodes/{nodeId}")
    public Result<Map<String, Object>> nodeDetail(@PathVariable Long nodeId) {
        return Result.success(kbService.getNodeDetail(nodeId));
    }

    @Operation(summary = "文章正文")
    @GetMapping("/articles/{articleId}")
    public Result<Map<String, Object>> article(@PathVariable Long articleId) {
        return Result.success(kbService.getArticle(articleId));
    }
}
