package com.wuyou.rag.entity.kb;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("kb_document")
public class KbDocument implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 文档ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属知识库ID */
    private Long kbId;

    /** 文档标题 */
    private String title;

    /** 文件扩展名，如 pdf/docx/txt/md */
    private String fileType;

    /** MinIO 存储URL */
    private String fileUrl;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 文档分块数量 */
    private Integer chunkCount = 0;

    /** 处理状态：0=待处理, 1=处理中, 2=完成, 3=失败 */
    private Integer status = 0;

    /** 处理失败错误信息 */
    private String errorMsg;

    /** 创建人用户ID */
    private Long createBy;

    /** 创建人名称（非数据库字段） */
    @TableField(exist = false)
    private String creatorName;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /** 软删除：0=正常, 1=已删除 */
    @TableLogic
    private Integer deleted;
}
