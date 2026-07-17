-- ============================================
-- V013__fix_embedding_dimension.sql — 修正 embedding 维度
-- ============================================
-- V007 创建 issue_embeddings 时使用了 vector(2000)（pgvector HNSW 上限），
-- 但实际使用的 BGE-M3 输出 1024 维。CAST 时 PG 拒绝尺寸不匹配的向量。
-- 改为 vector(1024) 匹配 BGE-M3，在限制内（HNSW 上限 2000）。
-- V004 约定：DB 只保留 PK+NOT NULL+DEFAULT。

ALTER TABLE issue_embeddings ALTER COLUMN embedding TYPE vector(1024);
