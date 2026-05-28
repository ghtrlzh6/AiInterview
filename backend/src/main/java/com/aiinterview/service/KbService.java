package com.aiinterview.service;

import java.util.List;
import java.util.Map;

public interface KbService {

    List<Map<String, Object>> getTree(Long parentId, String positionCode);

    Map<String, Object> getNodeDetail(Long nodeId);

    Map<String, Object> getArticle(Long articleId);
}
