package com.aiinterview.service.impl;

import com.aiinterview.common.BusinessException;
import com.aiinterview.entity.KbArticle;
import com.aiinterview.entity.KbNode;
import com.aiinterview.mapper.KbArticleMapper;
import com.aiinterview.mapper.KbNodeMapper;
import com.aiinterview.service.KbService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KbServiceImpl implements KbService {

    private final KbNodeMapper kbNodeMapper;
    private final KbArticleMapper kbArticleMapper;

    @Override
    public List<Map<String, Object>> getTree(Long parentId, String positionCode) {
        Long pid = parentId != null ? parentId : 1L;
        List<KbNode> nodes = kbNodeMapper.selectList(new LambdaQueryWrapper<KbNode>()
                .eq(KbNode::getParentId, pid)
                .eq(KbNode::getIsActive, 1)
                .orderByAsc(KbNode::getSortOrder));
        return nodes.stream()
                .filter(n -> matchPosition(n, positionCode))
                .map(this::toTreeNode)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getNodeDetail(Long nodeId) {
        KbNode node = kbNodeMapper.selectById(nodeId);
        if (node == null) {
            throw BusinessException.notFound("节点不存在");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("node", toTreeNode(node));
        result.put("breadcrumb", buildBreadcrumb(node));
        result.put("children", getTree(nodeId, null));
        List<KbArticle> articles = kbArticleMapper.selectList(new LambdaQueryWrapper<KbArticle>()
                .eq(KbArticle::getKbNodeId, nodeId)
                .orderByAsc(KbArticle::getDisplayOrder));
        result.put("articles", articles.stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.getId());
            m.put("title", a.getTitle());
            m.put("bodyPreview", truncate(a.getBodyMarkdown(), 200));
            return m;
        }).collect(Collectors.toList()));
        return result;
    }

    @Override
    public Map<String, Object> getArticle(Long articleId) {
        KbArticle article = kbArticleMapper.selectById(articleId);
        if (article == null) {
            throw BusinessException.notFound("文章不存在");
        }
        Map<String, Object> m = new HashMap<>();
        m.put("id", article.getId());
        m.put("kbNodeId", article.getKbNodeId());
        m.put("title", article.getTitle());
        m.put("bodyMarkdown", article.getBodyMarkdown());
        return m;
    }

    private List<Map<String, Object>> buildBreadcrumb(KbNode node) {
        List<Map<String, Object>> list = new ArrayList<>();
        KbNode current = node;
        while (current != null) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", current.getId());
            m.put("title", current.getTitle());
            list.add(m);
            if (current.getParentId() == null) break;
            current = kbNodeMapper.selectById(current.getParentId());
        }
        Collections.reverse(list);
        return list;
    }

    private Map<String, Object> toTreeNode(KbNode n) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", n.getId());
        m.put("parentId", n.getParentId());
        m.put("title", n.getTitle());
        m.put("slug", n.getSlug());
        m.put("nodeType", n.getNodeType());
        m.put("summaryExcerpt", n.getSummaryExcerpt());
        m.put("hasChildren", kbNodeMapper.selectCount(new LambdaQueryWrapper<KbNode>()
                .eq(KbNode::getParentId, n.getId())) > 0);
        return m;
    }

    private boolean matchPosition(KbNode node, String positionCode) {
        if (!StringUtils.hasText(positionCode)) return true;
        if (node.getPositionCodes() == null || node.getPositionCodes().isEmpty()) return true;
        return node.getPositionCodes().contains(positionCode);
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
