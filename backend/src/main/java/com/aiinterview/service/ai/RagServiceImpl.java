package com.aiinterview.service.ai;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.aiinterview.entity.KbArticle;
import com.aiinterview.mapper.KbArticleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {

    private final LlmService llmService;
    private final KbArticleMapper kbArticleMapper;

    @Value("${ai.chroma.host:localhost}")
    private String chromaHost;

    @Value("${ai.chroma.port:8000}")
    private int chromaPort;

    @Value("${ai.chroma.collection-prefix:ai_interview_}")
    private String collectionPrefix;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    private static final String COLLECTION = "kb";

    @Override
    public boolean isAvailable() {
        return chromaHeartbeat();
    }

    @Override
    public VectorizeResult vectorizeArticle(Long articleId, Long kbNodeId, String title, String bodyMarkdown) {
        if (!StringUtils.hasText(bodyMarkdown)) {
            return new VectorizeResult(0, List.of(), true, "正文为空，跳过向量化");
        }
        List<String> chunks = chunkText(bodyMarkdown);
        if (chunks.isEmpty()) {
            return new VectorizeResult(0, List.of(), true, "无有效文本块");
        }
        List<String> ids = new ArrayList<>();
        List<List<Double>> embeddings = new ArrayList<>();
        List<String> documents = new ArrayList<>();
        List<JSONObject> metadatas = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            String chunkId = "article-" + articleId + "-chunk-" + i;
            ids.add(chunkId);
            documents.add(chunks.get(i));
            embeddings.add(llmService.embed(title + "\n" + chunks.get(i)));
            JSONObject meta = new JSONObject();
            meta.set("article_id", articleId);
            meta.set("kb_node_id", kbNodeId != null ? kbNodeId : 0);
            meta.set("article_title", title != null ? title : "");
            metadatas.add(meta);
        }
        if (!chromaHeartbeat()) {
            log.warn("Chroma unavailable, vectorize article {} in mock mode", articleId);
            return new VectorizeResult(chunks.size(), ids, true, "Chroma 未连接，已标记为已向量化（模拟）");
        }
        try {
            ensureCollection();
            JSONObject body = new JSONObject();
            body.set("ids", ids);
            body.set("embeddings", embeddings);
            body.set("documents", documents);
            body.set("metadatas", metadatas);
            Request request = new Request.Builder()
                    .url(baseUrl() + "/collections/" + collectionName() + "/add")
                    .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String err = response.body() != null ? response.body().string() : "unknown";
                    log.warn("Chroma add failed: {}", err);
                    return new VectorizeResult(chunks.size(), ids, true, "Chroma 写入失败，已降级为模拟");
                }
            }
            return new VectorizeResult(chunks.size(), ids, false, "向量化完成");
        } catch (Exception e) {
            log.warn("Vectorize failed for article {}", articleId, e);
            return new VectorizeResult(chunks.size(), ids, true, "向量化异常，已降级为模拟");
        }
    }

    @Override
    public List<RagChunk> search(String query, String positionCode, int topK) {
        if (!StringUtils.hasText(query) || !chromaHeartbeat()) {
            return List.of();
        }
        try {
            ensureCollection();
            List<Double> embedding = llmService.embed(query);
            JSONObject body = new JSONObject();
            body.set("query_embeddings", List.of(embedding));
            body.set("n_results", Math.max(1, topK));
            Request request = new Request.Builder()
                    .url(baseUrl() + "/collections/" + collectionName() + "/query")
                    .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return List.of();
                }
                String raw = response.body() != null ? response.body().string() : "{}";
                return parseQueryResult(raw);
            }
        } catch (Exception e) {
            log.debug("RAG search failed", e);
            return List.of();
        }
    }

    @Override
    public int vectorizePendingBatch() {
        List<KbArticle> pending = kbArticleMapper.selectList(new LambdaQueryWrapper<KbArticle>()
                .eq(KbArticle::getIsVectorized, 0));
        int processed = 0;
        for (KbArticle article : pending) {
            VectorizeResult result = vectorizeArticle(article.getId(), article.getKbNodeId(),
                    article.getTitle(), article.getBodyMarkdown());
            article.setIsVectorized(1);
            article.setChromaIds(result.chromaIds());
            kbArticleMapper.updateById(article);
            processed++;
        }
        return processed;
    }

    @Override
    public String buildContext(String query, String positionCode, int topK) {
        List<RagChunk> chunks = search(query, positionCode, topK);
        if (chunks.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("参考知识片段：\n");
        for (int i = 0; i < chunks.size(); i++) {
            RagChunk chunk = chunks.get(i);
            sb.append(i + 1).append(". ").append(chunk.document()).append("\n");
        }
        return sb.toString();
    }

    private List<RagChunk> parseQueryResult(String raw) {
        JSONObject json = JSONUtil.parseObj(raw);
        JSONArray documents = json.getByPath("documents[0]", JSONArray.class);
        JSONArray distances = json.getByPath("distances[0]", JSONArray.class);
        JSONArray metadatas = json.getByPath("metadatas[0]", JSONArray.class);
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        List<RagChunk> result = new ArrayList<>();
        for (int i = 0; i < documents.size(); i++) {
            String doc = documents.getStr(i, "");
            double distance = distances != null && i < distances.size() ? distances.getDouble(i) : 0;
            Long kbNodeId = null;
            String title = "";
            if (metadatas != null && i < metadatas.size()) {
                JSONObject meta = metadatas.getJSONObject(i);
                if (meta != null) {
                    kbNodeId = meta.getLong("kb_node_id");
                    title = meta.getStr("article_title", "");
                }
            }
            result.add(new RagChunk(doc, distance, kbNodeId, title));
        }
        return result;
    }

    private void ensureCollection() throws IOException {
        JSONObject body = new JSONObject();
        body.set("name", collectionName());
        body.set("get_or_create", true);
        Request request = new Request.Builder()
                .url(baseUrl() + "/collections")
                .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to ensure collection: " + response.code());
            }
        }
    }

    private boolean chromaHeartbeat() {
        Request request = new Request.Builder()
                .url(baseUrl() + "/heartbeat")
                .get()
                .build();
        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    private List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();
        String normalized = text.replace("\r\n", "\n").trim();
        if (!StringUtils.hasText(normalized)) {
            return chunks;
        }
        String[] paragraphs = normalized.split("\n\n+");
        StringBuilder current = new StringBuilder();
        for (String paragraph : paragraphs) {
            String piece = paragraph.trim();
            if (!StringUtils.hasText(piece)) {
                continue;
            }
            if (current.length() + piece.length() > 800) {
                if (current.length() > 0) {
                    chunks.add(current.toString().trim());
                    current = new StringBuilder();
                }
                if (piece.length() > 800) {
                    for (int i = 0; i < piece.length(); i += 800) {
                        chunks.add(piece.substring(i, Math.min(i + 800, piece.length())));
                    }
                } else {
                    current.append(piece);
                }
            } else {
                if (current.length() > 0) {
                    current.append("\n\n");
                }
                current.append(piece);
            }
        }
        if (current.length() > 0) {
            chunks.add(current.toString().trim());
        }
        return chunks;
    }

    private String collectionName() {
        return collectionPrefix + COLLECTION;
    }

    private String baseUrl() {
        return "http://" + chromaHost + ":" + chromaPort + "/api/v1";
    }
}
