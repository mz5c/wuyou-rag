package com.wuyou.rag.rag.vector;

import java.util.List;

public interface VectorService {

    void insertVectors(List<Long> chunkIds, List<float[]> embeddings);

    List<Long> search(float[] queryEmbedding, int topK);

    void deleteByDocId(Long docId);
}
