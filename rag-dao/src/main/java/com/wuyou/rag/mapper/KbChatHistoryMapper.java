package com.wuyou.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wuyou.rag.entity.kb.KbChatHistory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface KbChatHistoryMapper extends BaseMapper<KbChatHistory> {
}
