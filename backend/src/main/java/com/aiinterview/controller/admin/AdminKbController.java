package com.aiinterview.controller.admin;

import com.aiinterview.common.BusinessException;
import com.aiinterview.common.Result;
import com.aiinterview.entity.KbArticle;
import com.aiinterview.entity.KbNode;
import com.aiinterview.mapper.KbArticleMapper;
import com.aiinterview.mapper.KbNodeMapper;
import com.aiinterview.service.ai.RagService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "管理员-知识库")
@RestController
@RequestMapping("/api/v1/admin/kb")
@RequiredArgsConstructor
public class AdminKbController {

    private final KbNodeMapper kbNodeMapper;
    private final KbArticleMapper kbArticleMapper;
    private final RagService ragService;

    @GetMapping("/nodes")
    public Result<List<Map<String, Object>>> listNodes(
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<KbNode> w = new LambdaQueryWrapper<>();
        if (parentId != null) w.eq(KbNode::getParentId, parentId);
        if (StringUtils.hasText(keyword)) w.like(KbNode::getTitle, keyword);
        w.orderByAsc(KbNode::getSortOrder);
        List<Map<String, Object>> list = kbNodeMapper.selectList(w).stream().map(this::nodeMap).collect(Collectors.toList());
        return Result.success(list);
    }

    @PostMapping("/nodes")
    public Result<Map<String, Object>> createNode(@RequestBody NodeRequest req) {
        KbNode node = new KbNode();
        node.setParentId(req.parentId);
        node.setTitle(req.title);
        node.setSlug(req.slug);
        node.setNodeType(req.nodeType);
        node.setPositionCodes(req.positionCodes);
        node.setSummaryExcerpt(req.summaryExcerpt != null ? req.summaryExcerpt : "");
        node.setIsActive(1);
        node.setDepth(req.parentId != null ? 1 : 0);
        kbNodeMapper.insert(node);
        return Result.success(Map.of("id", node.getId()));
    }

    @PutMapping("/nodes/{nodeId}")
    public Result<Void> updateNode(@PathVariable Long nodeId, @RequestBody NodeRequest req) {
        KbNode node = kbNodeMapper.selectById(nodeId);
        if (node == null) throw BusinessException.notFound("节点不存在");
        if (req.title != null) node.setTitle(req.title);
        if (req.slug != null) node.setSlug(req.slug);
        if (req.positionCodes != null) node.setPositionCodes(req.positionCodes);
        if (req.summaryExcerpt != null) node.setSummaryExcerpt(req.summaryExcerpt);
        if (req.sortOrder != null) node.setSortOrder(req.sortOrder);
        if (req.isActive != null) node.setIsActive(req.isActive ? 1 : 0);
        kbNodeMapper.updateById(node);
        return Result.success();
    }

    @DeleteMapping("/nodes/{nodeId}")
    public Result<Void> deleteNode(@PathVariable Long nodeId) {
        kbNodeMapper.deleteById(nodeId);
        return Result.success();
    }

    @PostMapping("/articles")
    public Result<Map<String, Object>> createArticle(@RequestBody ArticleRequest req) {
        KbArticle article = new KbArticle();
        article.setKbNodeId(req.kbNodeId);
        article.setTitle(req.title != null ? req.title : "");
        article.setBodyMarkdown(req.bodyMarkdown);
        article.setDisplayOrder(req.displayOrder != null ? req.displayOrder : 0);
        article.setIsVectorized(0);
        kbArticleMapper.insert(article);
        return Result.success(Map.of("id", article.getId()));
    }

    @PutMapping("/articles/{articleId}")
    public Result<Void> updateArticle(@PathVariable Long articleId, @RequestBody ArticleRequest req) {
        KbArticle article = kbArticleMapper.selectById(articleId);
        if (article == null) throw BusinessException.notFound("文章不存在");
        if (req.title != null) article.setTitle(req.title);
        if (req.bodyMarkdown != null) article.setBodyMarkdown(req.bodyMarkdown);
        if (req.displayOrder != null) article.setDisplayOrder(req.displayOrder);
        kbArticleMapper.updateById(article);
        return Result.success();
    }

    @DeleteMapping("/articles/{articleId}")
    public Result<Void> deleteArticle(@PathVariable Long articleId) {
        kbArticleMapper.deleteById(articleId);
        return Result.success();
    }

    @PostMapping("/articles/{articleId}/vectorize")
    public Result<Map<String, Object>> vectorize(@PathVariable Long articleId) {
        KbArticle article = kbArticleMapper.selectById(articleId);
        if (article == null) throw BusinessException.notFound("文章不存在");
        RagService.VectorizeResult result = ragService.vectorizeArticle(
                article.getId(), article.getKbNodeId(), article.getTitle(), article.getBodyMarkdown());
        article.setIsVectorized(1);
        article.setChromaIds(result.chromaIds());
        kbArticleMapper.updateById(article);
        Map<String, Object> m = new HashMap<>();
        m.put("articleId", articleId);
        m.put("chunksCount", result.chunksCount());
        m.put("mock", result.mock());
        m.put("message", result.message());
        return Result.success(m);
    }

    @PostMapping("/vectorize-pending-batch")
    public Result<Map<String, Object>> vectorizeBatch() {
        int processed = ragService.vectorizePendingBatch();
        return Result.success(Map.of("processed", processed, "chromaAvailable", ragService.isAvailable()));
    }

    private Map<String, Object> nodeMap(KbNode n) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", n.getId());
        m.put("parentId", n.getParentId());
        m.put("title", n.getTitle());
        m.put("slug", n.getSlug());
        m.put("nodeType", n.getNodeType());
        m.put("positionCodes", n.getPositionCodes());
        m.put("isActive", n.getIsActive());
        return m;
    }

    @Data
    public static class NodeRequest {
        private Long parentId;
        private String title;
        private String slug;
        private String nodeType;
        private List<String> positionCodes;
        private String summaryExcerpt;
        private Integer sortOrder;
        private Boolean isActive;
    }

    @Data
    public static class ArticleRequest {
        private Long kbNodeId;
        private String title;
        private String bodyMarkdown;
        private Integer displayOrder;
    }
}
