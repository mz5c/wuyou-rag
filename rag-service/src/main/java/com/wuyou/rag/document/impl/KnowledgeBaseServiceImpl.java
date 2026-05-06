package com.wuyou.rag.document.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wuyou.rag.document.KnowledgeBaseService;
import com.wuyou.rag.entity.kb.KbKnowledgeBase;
import com.wuyou.rag.exception.ErrorCode;
import com.wuyou.rag.mapper.KbKnowledgeBaseMapper;
import com.wuyou.rag.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KbKnowledgeBaseMapper kbKnowledgeBaseMapper;

    @Override
    public Result<List<KbKnowledgeBase>> listAll() {
        List<KbKnowledgeBase> list = kbKnowledgeBaseMapper.selectList(
                Wrappers.<KbKnowledgeBase>lambdaQuery()
                        .orderByDesc(KbKnowledgeBase::getCreateTime));
        return Result.success(list);
    }

    @Override
    public Result<KbKnowledgeBase> getById(Long id) {
        KbKnowledgeBase kb = kbKnowledgeBaseMapper.selectById(id);
        if (kb == null) {
            return Result.fail(ErrorCode.NOT_FOUND.getCode(), ErrorCode.NOT_FOUND.getMessage());
        }
        return Result.success(kb);
    }

    @Override
    public Result<Void> create(String name, String description, Long createBy) {
        KbKnowledgeBase kb = new KbKnowledgeBase();
        kb.setName(name);
        kb.setDescription(description);
        kb.setCreateBy(createBy);
        kb.setStatus(1);
        kbKnowledgeBaseMapper.insert(kb);
        log.info("Knowledge base created: name={}, createBy={}", name, createBy);
        return Result.success(null);
    }

    @Override
    public Result<Void> update(Long id, String name, String description) {
        KbKnowledgeBase existing = kbKnowledgeBaseMapper.selectById(id);
        if (existing == null) {
            return Result.fail(ErrorCode.NOT_FOUND.getCode(), ErrorCode.NOT_FOUND.getMessage());
        }
        KbKnowledgeBase kb = new KbKnowledgeBase();
        kb.setId(id);
        kb.setName(name);
        kb.setDescription(description);
        kbKnowledgeBaseMapper.updateById(kb);
        log.info("Knowledge base updated: id={}, name={}", id, name);
        return Result.success(null);
    }

    @Override
    public Result<Void> delete(Long id) {
        KbKnowledgeBase existing = kbKnowledgeBaseMapper.selectById(id);
        if (existing == null) {
            return Result.fail(ErrorCode.NOT_FOUND.getCode(), ErrorCode.NOT_FOUND.getMessage());
        }
        kbKnowledgeBaseMapper.deleteById(id);
        log.info("Knowledge base deleted: id={}, name={}", id, existing.getName());
        return Result.success(null);
    }
}
