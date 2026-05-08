package com.wuyou.rag.entity.kb;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("kb_chat_history")
public class KbChatHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 聊天记录ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属会话ID */
    private Long conversationId;

    /** 用户ID */
    private Long userId;

    /** 用户提问 */
    private String question;

    /** 模型回答 */
    private String answer;

    /** 回答类型：llm=模型生成, exact=缓存命中 */
    private String answerType = "llm";

    /** 思考内容（reasoning） */
    private String reasoningContent;

    /** 检索到的分块ID列表，逗号分隔 */
    private String usedChunkIds;

    /** 引用来源JSON，包含文档标题和分块内容 */
    private String sources;

    /** 用户反馈：1=点赞, 0=点踩, NULL=未评价 */
    private Integer feedback;

    /** 反馈备注 */
    private String feedbackComment;

    /** 响应耗时（毫秒） */
    private Integer elapsedMs;

    /** 消耗Token数 */
    private Integer tokensUsed;

    /** 逻辑删除：0=正常, 1=已删除 */
    @TableLogic
    @JsonIgnore
    private Integer deleted;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
