package com.wuyou.rag.rag.embedding;

import java.util.List;

public interface EmbeddingService {

    float[] embed(String text);

    List<float[]> embed(List<String> texts);
}
