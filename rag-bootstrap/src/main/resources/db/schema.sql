-- =====================================================
-- 企业级RAG智能知识库 - 数据库初始化脚本
-- =====================================================
CREATE DATABASE IF NOT EXISTS `wy-rag` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `wy-rag`;

-- 用户表（Spring Security）
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(32)  NOT NULL UNIQUE,
    password    VARCHAR(128) NOT NULL,
    nickname    VARCHAR(64),
    role        VARCHAR(16)  NOT NULL DEFAULT 'USER',
    status      TINYINT      NOT NULL DEFAULT 1,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 知识库分类表
CREATE TABLE IF NOT EXISTS kb_knowledge_base (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(128) NOT NULL,
    description VARCHAR(512),
    status      TINYINT      NOT NULL DEFAULT 1,
    create_by   BIGINT,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库分类表';

-- 文档表
CREATE TABLE IF NOT EXISTS kb_document (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    kb_id        BIGINT       NOT NULL,
    title        VARCHAR(256) NOT NULL,
    file_type    VARCHAR(16)  NOT NULL,
    file_url     VARCHAR(512) NOT NULL,
    file_size    BIGINT,
    chunk_count  INT          DEFAULT 0,
    status       TINYINT      NOT NULL DEFAULT 0,
    error_msg    VARCHAR(512),
    create_by    BIGINT,
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_kb_id (kb_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档表';

-- 文本分块表
CREATE TABLE IF NOT EXISTS kb_chunk (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    doc_id         BIGINT       NOT NULL,
    chunk_content  TEXT         NOT NULL,
    chunk_index    INT          NOT NULL,
    chunk_size     INT,
    vector_id      VARCHAR(64),
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_doc_id (doc_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文本分块表';

-- 对话会话表
CREATE TABLE IF NOT EXISTS kb_conversation (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT       NOT NULL,
    title          VARCHAR(256) DEFAULT '新对话',
    kb_id          BIGINT,
    message_count  INT          DEFAULT 0,
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
    answer_type      VARCHAR(8)   DEFAULT 'llm',
    used_chunk_ids   TEXT,
    sources          JSON,
    feedback         TINYINT,
    feedback_comment VARCHAR(256),
    elapsed_ms       INT,
    tokens_used      INT,
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conv_id (conversation_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问答历史表';

-- 审计日志表
CREATE TABLE IF NOT EXISTS kb_audit_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT,
    username    VARCHAR(32),
    operation   VARCHAR(64)  NOT NULL,
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
    config_key   VARCHAR(64)  NOT NULL UNIQUE,
    config_value VARCHAR(256) NOT NULL,
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
