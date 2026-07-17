-- ============================================
-- V007__create_issue_embeddings.sql — 指摘向量嵌入表
-- ============================================
-- V004 约定：数据库只保留 PK + NOT NULL + DEFAULT。
-- 例外：HNSW 索引是向量搜索必需的底层技术索引，非业务索引。

CREATE TABLE issue_embeddings (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    finding_id  UUID NOT NULL,
    content     TEXT NOT NULL,
    embedding   vector(2000) NOT NULL,
    model_name  VARCHAR(50) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- HNSW 索引 — 向量余弦相似度搜索，唯一技术索引例外
CREATE INDEX idx_issue_embeddings_hnsw
    ON issue_embeddings
    USING hnsw (embedding vector_cosine_ops);
