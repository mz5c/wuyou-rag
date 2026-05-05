package com.wuyou.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wuyou.rag.entity.kb.KbConversation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface KbConversationMapper extends BaseMapper<KbConversation> {
}
