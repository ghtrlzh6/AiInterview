package com.aiinterview.service.ai;

import java.util.List;

public interface RagService {

    boolean isAvailable();

    VectorizeResult vectorizeArticle(Long articleId, Long kbNodeId, String title, String bodyMarkdown);

    List<RagChunk> search(String query, String positionCode, int topK);

    int vectorizePendingBatch();

    String buildContext(String query, String positionCode, int topK);

    record RagChunk(String document, double distance, Long kbNodeId, String articleTitle) {
    }

    record VectorizeResult(int chunksCount, List<String> chromaIds, boolean mock, String message) {
    }
}
