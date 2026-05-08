package com.wuyou.rag.entity.kb;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("kb_chunk")
public class KbChunk implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 分块ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属文档ID */
    private Long docId;

    /** 分块文本内容 */
    private String chunkContent;

    /** 在文档中的序号，从0开始 */
    private Integer chunkIndex;

    /** 分块字符数 */
    private Integer chunkSize;

    /** Milvus 向量ID */
    private String vectorId;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
