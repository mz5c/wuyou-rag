-- =====================================================
-- 内部RAG智能知识库 - 数据库初始化脚本
-- =====================================================
CREATE DATABASE IF NOT EXISTS `wy_rag` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `wy_rag`;

-- 用户表（Spring Security）
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(32)  NOT NULL UNIQUE,
    password    VARCHAR(128) NOT NULL,
    nickname    VARCHAR(64),
    role        VARCHAR(16)  NOT NULL DEFAULT 'USER' COMMENT '角色：ADMIN=管理员, USER=普通用户',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1=启用, 0=禁用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除：0=正常, 1=已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 知识库分类表
CREATE TABLE IF NOT EXISTS kb_knowledge_base (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(128) NOT NULL,
    description VARCHAR(512),
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1=启用, 0=禁用',
    create_by   BIGINT,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除：0=正常, 1=已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库分类表';

-- 文档表
CREATE TABLE IF NOT EXISTS kb_document (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    kb_id        BIGINT       NOT NULL COMMENT '所属知识库ID',
    title        VARCHAR(256) NOT NULL,
    file_type    VARCHAR(16)  NOT NULL COMMENT '文件扩展名，如 pdf/docx/txt/md',
    file_url     VARCHAR(512) NOT NULL COMMENT 'MinIO 存储URL',
    file_size    BIGINT COMMENT '文件大小（字节）',
    chunk_count  INT          DEFAULT 0 COMMENT '文档分块数量',
    status       TINYINT      NOT NULL DEFAULT 0 COMMENT '处理状态：0=待处理, 1=处理中, 2=完成, 3=失败',
    error_msg    VARCHAR(512),
    create_by    BIGINT,
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除：0=正常, 1=已删除',
    INDEX idx_kb_id (kb_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档表';

-- 文本分块表
CREATE TABLE IF NOT EXISTS kb_chunk (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    doc_id         BIGINT       NOT NULL,
    chunk_content  TEXT         NOT NULL,
    chunk_index    INT          NOT NULL,
    chunk_size     INT,
    vector_id      VARCHAR(64) COMMENT 'Milvus 向量ID',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_doc_id (doc_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文本分块表';

-- 对话会话表
CREATE TABLE IF NOT EXISTS kb_conversation (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT       NOT NULL,
    title          VARCHAR(256) DEFAULT '新对话',
    kb_id          BIGINT COMMENT '关联知识库ID（可空）',
    message_count  INT          DEFAULT 0,
    deleted        TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除：0=正常, 1=已删除',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话会话表';

-- 问答历史表
CREATE TABLE IF NOT EXISTS kb_chat_history (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id  BIGINT       NOT NULL,
    user_id          BIGINT       NOT NULL,
    question         TEXT         NOT NULL,
    answer           LONGTEXT,
    answer_type      VARCHAR(8)   DEFAULT 'llm' COMMENT '回答类型：llm=模型生成, exact=缓存命中',
    reasoning_content LONGTEXT COMMENT 'LLM 思考内容',
    used_chunk_ids   TEXT,
    sources          JSON,
    feedback         TINYINT COMMENT '用户反馈：1=点赞, 0=点踩, NULL=未评价',
    feedback_comment VARCHAR(256),
    elapsed_ms       INT,
    tokens_used      INT,
    deleted          TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除：0=正常, 1=已删除',
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conv_id (conversation_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问答历史表';

-- 审计日志表
CREATE TABLE IF NOT EXISTS kb_audit_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT,
    username    VARCHAR(32),
    operation   VARCHAR(64)  NOT NULL COMMENT '操作类型：LOGIN/LOGOUT/UPLOAD_DOC/DELETE_DOC/CREATE_KB/DELETE_KB/CONFIG_UPDATE/USER_MANAGE/CHAT',
    detail      VARCHAR(1024),
    ip          VARCHAR(64),
    user_agent  VARCHAR(256),
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_operation (operation),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';

-- 系统配置表
CREATE TABLE IF NOT EXISTS kb_config (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key   VARCHAR(64)  NOT NULL UNIQUE COMMENT '配置键，如 llm.api_url',
    config_value VARCHAR(256) NOT NULL COMMENT '配置值',
    description  VARCHAR(256),
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- =====================================================
-- 预置配置数据
-- =====================================================
INSERT IGNORE INTO kb_config (config_key, config_value, description) VALUES
('llm.api_url', 'http://localhost:8000/v1', '大模型 API 地址'),
('llm.model_name', 'qwen', '大模型名称'),
('llm.temperature', '0.7', '生成温度'),
('llm.timeout', '30000', '调用超时（毫秒）'),
('llm.max_retries', '2', '最大重试次数'),
('embedding.api_url', 'http://localhost:5001/embed', 'Embedding API 地址'),
('embedding.model_name', 'bge-large-zh-v1.5', 'Embedding 模型名称'),
('embedding.dimensions', '1024', '向量维度'),
('chunk.max_size', '1024', '分块最大字符数'),
('chunk.overlap', '128', '分块重叠窗口字符数'),
('milvus.host', 'localhost', 'Milvus 主机地址'),
('milvus.port', '19530', 'Milvus 端口'),
('milvus.collection', 'document_chunks', 'Milvus 集合名称'),
('search.top_k', '5', '检索返回 TOP K'),
('cache.ttl_hot_qa', '3600', '热点问答缓存秒数'),
('sensitive_words', '', '敏感词列表（逗号分隔）');

-- ES 配置
INSERT IGNORE INTO kb_config (config_key, config_value, description) VALUES
('es.host', 'localhost', 'ES 主机地址'),
('es.port', '9200', 'ES HTTP 端口'),
('search.hybrid.enabled', 'true', '是否启用混合检索'),
('search.hybrid.milvus_top_k', '20', 'Milvus 初筛返回数量'),
('search.hybrid.es_top_k', '20', 'ES BM25 初筛返回数量'),
('search.hybrid.final_top_k', '5', 'RRF 融合后最终 topN'),
('search.hybrid.rrf_k', '60', 'RRF 公式常数 k');

-- =====================================================
-- 迁移脚本（对已有数据库增量添加字段，新库已含在 CREATE TABLE 中）
-- 重复执行会报字段已存在的错，但 spring.sql.init.continue-on-error=true 会忽略
-- =====================================================
ALTER TABLE kb_conversation ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0 COMMENT '软删除：0=正常, 1=已删除';
ALTER TABLE kb_chat_history ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0 COMMENT '软删除：0=正常, 1=已删除';
ALTER TABLE kb_chat_history ADD COLUMN reasoning_content LONGTEXT COMMENT 'LLM 思考内容';
