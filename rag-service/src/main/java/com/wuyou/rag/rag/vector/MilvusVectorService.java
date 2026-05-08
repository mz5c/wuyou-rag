package com.wuyou.rag.rag.vector;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wuyou.rag.entity.kb.KbChunk;
import com.wuyou.rag.entity.kb.KbConfig;
import com.wuyou.rag.mapper.KbChunkMapper;
import com.wuyou.rag.mapper.KbConfigMapper;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.index.request.CreateIndexReq;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.response.SearchResp;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MilvusVectorService implements VectorService {

    private static final String COLLECTION_NAME = "document_chunks";
    private static final int VECTOR_DIMENSION = 1024;

    private final KbConfigMapper kbConfigMapper;
    private final KbChunkMapper kbChunkMapper;

    private MilvusClientV2 client;

    @PostConstruct
    public void init() {
        KbConfig hostConfig = kbConfigMapper.selectOne(
                Wrappers.<KbConfig>lambdaQuery().eq(KbConfig::getConfigKey, "milvus.host"));
        KbConfig portConfig = kbConfigMapper.selectOne(
                Wrappers.<KbConfig>lambdaQuery().eq(KbConfig::getConfigKey, "milvus.port"));

        String host = hostConfig != null ? hostConfig.getConfigValue() : "localhost";
        int port = portConfig != null ? Integer.parseInt(portConfig.getConfigValue()) : 19530;

        ConnectConfig config = ConnectConfig.builder()
                .uri("http://" + host + ":" + port)
                .username("root")
                .password("Milvus")
                .build();
        client = new MilvusClientV2(config);

        boolean exists = client.hasCollection(HasCollectionReq.builder()
                .collectionName(COLLECTION_NAME)
                .build());

        if (!exists) {
            CreateCollectionReq.CollectionSchema schema = CreateCollectionReq.CollectionSchema.builder().build();
            schema.addField(AddFieldReq.builder()
                    .fieldName("id").dataType(DataType.Int64).isPrimaryKey(true).autoID(true).build());
            schema.addField(AddFieldReq.builder()
                    .fieldName("chunk_id").dataType(DataType.Int64).build());
            schema.addField(AddFieldReq.builder()
                    .fieldName("embedding").dataType(DataType.FloatVector).dimension(VECTOR_DIMENSION).build());

            CreateCollectionReq createReq = CreateCollectionReq.builder()
                    .collectionName(COLLECTION_NAME)
                    .collectionSchema(schema)
                    .build();
            client.createCollection(createReq);

            IndexParam indexParam = IndexParam.builder()
                    .fieldName("embedding")
                    .indexType(IndexParam.IndexType.IVF_FLAT)
                    .metricType(IndexParam.MetricType.IP)
                    .extraParams(Collections.singletonMap("nlist", 128))
                    .build();

            CreateIndexReq indexReq = CreateIndexReq.builder()
                    .collectionName(COLLECTION_NAME)
                    .indexParams(Collections.singletonList(indexParam))
                    .build();
            client.createIndex(indexReq);

            log.info("Milvus collection created: {}", COLLECTION_NAME);
        } else {
            log.info("Milvus collection already exists: {}", COLLECTION_NAME);
        }
    }

    @Override
    public void insertVectors(List<Long> chunkIds, List<float[]> embeddings) {
        List<JSONObject> rows = new ArrayList<>();
        for (int i = 0; i < chunkIds.size(); i++) {
            JSONObject row = new JSONObject();
            row.put("chunk_id", chunkIds.get(i));
            List<Float> embeddingList = new ArrayList<>(embeddings.get(i).length);
            for (float v : embeddings.get(i)) {
                embeddingList.add(v);
            }
            row.put("embedding", embeddingList);
            rows.add(row);
        }

        InsertReq insertReq = InsertReq.builder()
                .collectionName(COLLECTION_NAME)
                .data(rows)
                .build();
        client.insert(insertReq);
        log.debug("Inserted {} vectors into Milvus", chunkIds.size());
    }

    @Override
    public List<Long> search(float[] queryEmbedding, int topK) {
        List<List<Float>> queryVectors = new ArrayList<>();
        List<Float> vector = new ArrayList<>();
        for (float v : queryEmbedding) {
            vector.add(v);
        }
        queryVectors.add(vector);

        Map<String, Object> searchParams = new HashMap<>();
        searchParams.put("nprobe", 10);

        SearchReq searchReq = SearchReq.builder()
                .collectionName(COLLECTION_NAME)
                .annsField("embedding")
                .data(queryVectors)
                .topK(topK)
                .searchParams(searchParams)
                .outputFields(Collections.singletonList("chunk_id"))
                .build();
        SearchResp searchResp = client.search(searchReq);

        List<Long> chunkIds = new ArrayList<>();
        for (List<SearchResp.SearchResult> results : searchResp.getSearchResults()) {
            for (SearchResp.SearchResult result : results) {
                Object chunkIdObj = result.getEntity().get("chunk_id");
                if (chunkIdObj instanceof Number) {
                    chunkIds.add(((Number) chunkIdObj).longValue());
                }
            }
        }
        return chunkIds;
    }

    @Override
    public void deleteByDocId(Long docId) {
        List<KbChunk> chunks = kbChunkMapper.selectList(
                Wrappers.<KbChunk>lambdaQuery().eq(KbChunk::getDocId, docId));
        if (chunks.isEmpty()) {
            return;
        }
        List<Long> chunkIds = chunks.stream().map(KbChunk::getId).collect(Collectors.toList());

        String filter = "chunk_id in [" + chunkIds.stream()
                .map(String::valueOf).collect(Collectors.joining(",")) + "]";

        DeleteReq deleteReq = DeleteReq.builder()
                .collectionName(COLLECTION_NAME)
                .filter(filter)
                .build();
        client.delete(deleteReq);
        log.info("Deleted {} vectors from Milvus for docId={}", chunkIds.size(), docId);
    }
}
