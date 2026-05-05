package com.wuyou.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wuyou.rag.entity.kb.KbChunk;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface KbChunkMapper extends BaseMapper<KbChunk> {
}
