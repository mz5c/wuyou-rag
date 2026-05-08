package com.wuyou.rag.entity.kb;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("kb_audit_log")
public class KbAuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 日志ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作用户ID */
    private Long userId;

    /** 操作用户名 */
    private String username;

    /** 操作类型：LOGIN/LOGOUT/UPLOAD_DOC/DELETE_DOC/CREATE_KB/DELETE_KB/CONFIG_UPDATE/USER_MANAGE/CHAT */
    private String operation;

    /** 操作详情JSON */
    private String detail;

    /** 操作IP地址 */
    private String ip;

    /** 用户代理（浏览器UA） */
    private String userAgent;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
