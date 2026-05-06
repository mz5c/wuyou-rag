package com.wuyou.rag.document;

import com.wuyou.rag.entity.kb.KbKnowledgeBase;
import com.wuyou.rag.result.Result;

import java.util.List;

public interface KnowledgeBaseService {

    Result<List<KbKnowledgeBase>> listAll();

    Result<KbKnowledgeBase> getById(Long id);

    Result<Void> create(String name, String description, Long createBy);

    Result<Void> update(Long id, String name, String description);

    Result<Void> delete(Long id);
}
