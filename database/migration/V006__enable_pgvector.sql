-- ============================================
-- V006__enable_pgvector.sql — 启用 pgvector 扩展
-- ============================================
-- pgvector 提供向量数据类型和索引，用于 AI 语义搜索。
-- 扩展名是 "vector"（非 "pgvector"）。

CREATE EXTENSION IF NOT EXISTS vector;
