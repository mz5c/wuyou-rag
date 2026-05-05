package com.wuyou.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wuyou.rag.entity.kb.KbConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface KbConfigMapper extends BaseMapper<KbConfig> {
}
