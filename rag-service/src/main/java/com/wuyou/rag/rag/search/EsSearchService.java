package com.wuyou.rag.rag.search;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wuyou.rag.entity.kb.KbConfig;
import com.wuyou.rag.mapper.KbConfigMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EsSearchService {

    private static final String INDEX_NAME = "kb_chunks";
    private static final String SETTINGS = """
            {
              "settings": {
                "number_of_shards": 1,
                "number_of_replicas": 0,
                "analysis": {
                  "analyzer": {
                    "ik_analyzer": {
                      "type": "custom",
                      "tokenizer": "ik_max_word"
                    }
                  }
                }
              },
              "mappings": {
                "properties": {
                  "chunk_id":   { "type": "long" },
                  "doc_id":     { "type": "long" },
                  "kb_id":      { "type": "long" },
                  "content":    { "type": "text", "analyzer": "ik_max_word" },
                  "create_time":{"type": "date", "format": "yyyy-MM-dd HH:mm:ss"}
                }
              }
            }
            """;

    private final RestTemplate restTemplate;
    private final KbConfigMapper kbConfigMapper;
    private final ObjectMapper objectMapper;

    private String esUrl;

    @PostConstruct
    public void init() {
        KbConfig hostConfig = kbConfigMapper.selectOne(
                Wrappers.<KbConfig>lambdaQuery().eq(KbConfig::getConfigKey, "es.host"));
        KbConfig portConfig = kbConfigMapper.selectOne(
                Wrappers.<KbConfig>lambdaQuery().eq(KbConfig::getConfigKey, "es.port"));

        String host = hostConfig != null ? hostConfig.getConfigValue() : "localhost";
        int port = portConfig != null ? Integer.parseInt(portConfig.getConfigValue()) : 9200;
        esUrl = "http://" + host + ":" + port;

        createIndexIfNotExists();
    }

    private void createIndexIfNotExists() {
        try {
            String url = esUrl + "/" + INDEX_NAME;
            restTemplate.execute(url, org.springframework.http.HttpMethod.HEAD, null, null);
            log.info("ES index already exists: {}", INDEX_NAME);
        } catch (Exception e) {
            try {
                restTemplate.put(esUrl + "/" + INDEX_NAME, SETTINGS, String.class);
                log.info("ES index created: {}", INDEX_NAME);
            } catch (Exception ex) {
                log.warn("Failed to create ES index, ES may be unavailable: {}", ex.getMessage());
            }
        }
    }

    /** Index a single chunk document */
    public void indexChunk(Long chunkId, Long docId, String content, Long kbId) {
        try {
            ObjectNode doc = objectMapper.createObjectNode();
            doc.put("chunk_id", chunkId);
            doc.put("doc_id", docId);
            doc.put("kb_id", kbId);
            doc.put("content", content);
            doc.put("create_time", java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            restTemplate.postForEntity(
                    esUrl + "/" + INDEX_NAME + "/_doc/" + chunkId,
                    doc,
                    String.class);
        } catch (Exception e) {
            log.warn("Failed to index chunk to ES: chunkId={}, docId={}", chunkId, docId, e);
        }
    }

    /** BM25 keyword search */
    public List<EsSearchHit> search(String query, Long kbId, int topK) {
        try {
            ObjectNode queryBody = objectMapper.createObjectNode();

            // bool query: must match content, filter by kb_id
            ObjectNode boolNode = objectMapper.createObjectNode();
            ArrayNode mustArray = boolNode.putArray("must");
            ObjectNode matchNode = objectMapper.createObjectNode();
            ObjectNode matchContent = objectMapper.createObjectNode();
            matchContent.put("content", query);
            matchNode.set("match", matchContent);
            mustArray.add(matchNode);

            ArrayNode filterArray = boolNode.putArray("filter");
            ObjectNode termNode = objectMapper.createObjectNode();
            ObjectNode termKbId = objectMapper.createObjectNode();
            termKbId.put("kb_id", kbId);
            termNode.set("term", termKbId);
            filterArray.add(termNode);

            queryBody.set("query", objectMapper.createObjectNode().set("bool", boolNode));
            queryBody.put("size", topK);

            // Request _source fields: chunk_id, content, doc_id
            ObjectNode sourceNode = objectMapper.createObjectNode();
            ArrayNode includes = sourceNode.putArray("includes");
            includes.add("chunk_id");
            includes.add("content");
            includes.add("doc_id");
            queryBody.set("_source", sourceNode);

            String url = esUrl + "/" + INDEX_NAME + "/_search";
            String response = restTemplate.postForEntity(url, queryBody, String.class).getBody();
            return parseSearchResponse(response, topK);
        } catch (Exception e) {
            log.warn("ES search failed, query={}", query, e);
            return new ArrayList<>();
        }
    }

    private List<EsSearchHit> parseSearchResponse(String response, int topK) {
        List<EsSearchHit> hits = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode hitsNode = root.path("hits").path("hits");
            for (JsonNode hit : hitsNode) {
                long chunkId = hit.path("_source").path("chunk_id").asLong();
                String content = hit.path("_source").path("content").asText();
                long docId = hit.path("_source").path("doc_id").asLong();
                double score = hit.path("_score").asDouble();
                hits.add(new EsSearchHit(chunkId, docId, content, score));
            }
        } catch (Exception e) {
            log.warn("Failed to parse ES search response", e);
        }
        return hits;
    }

    /** Delete all chunks belonging to a document */
    public void deleteByDocId(Long docId) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            ObjectNode term = objectMapper.createObjectNode();
            ObjectNode termValue = objectMapper.createObjectNode();
            termValue.put("doc_id", docId);
            term.set("term", termValue);
            body.set("query", objectMapper.createObjectNode().set("term", term));

            restTemplate.postForEntity(
                    esUrl + "/" + INDEX_NAME + "/_delete_by_query",
                    body,
                    String.class);
            log.info("Deleted ES docs by docId={}", docId);
        } catch (Exception e) {
            log.warn("Failed to delete ES docs by docId={}", docId, e);
        }
    }

    /** Delete a single chunk */
    public void deleteChunk(Long chunkId) {
        try {
            restTemplate.delete(esUrl + "/" + INDEX_NAME + "/_doc/" + chunkId);
        } catch (Exception e) {
            log.warn("Failed to delete ES chunk: chunkId={}", chunkId, e);
        }
    }

    /** Check if ES is available */
    public boolean isAvailable() {
        try {
            restTemplate.getForEntity(esUrl + "/_cluster/health", String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public record EsSearchHit(Long chunkId, Long docId, String content, double score) {}
}
