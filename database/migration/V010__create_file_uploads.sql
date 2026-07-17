-- ============================================
-- V010__create_file_uploads.sql — 文件上传记录表
-- ============================================

CREATE TABLE file_uploads (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),  -- 主键，自动生成
    source_type   VARCHAR(20),                                -- 来源类型
    original_name VARCHAR(255) NOT NULL,                      -- 上传时的原始文件名
    stored_path   VARCHAR(500) NOT NULL,                      -- 存储路径（MinIO 对象键）
    mime          VARCHAR(100),                               -- MIME 类型
    size          BIGINT,                                     -- 文件大小（字节）
    uploaded_by   VARCHAR(100),                               -- 上传者标识
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()         -- 创建时间
);
