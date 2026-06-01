package com.wuyou.rag.rag.vector;

import java.util.List;

public interface VectorService {

    void insertVectors(List<Long> chunkIds, List<float[]> embeddings, Long kbId);

    List<Long> search(float[] queryEmbedding, int topK, Long kbId);

    void deleteByDocId(Long docId);
}
