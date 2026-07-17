-- ============================================
-- V005__create_findings.sql — 核心业务表：指摘
-- ============================================
-- V004 约定：数据库只保留 PK + NOT NULL + DEFAULT，
-- 不添加业务索引、CHECK 约束、触发器，由后端应用层负责。

CREATE TABLE findings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title           TEXT NOT NULL,
    description     TEXT,
    category        TEXT,
    priority        TEXT,
    severity        TEXT,
    system          TEXT,
    assignee        TEXT,
    tags            TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    source_type     VARCHAR(20),
    source_file_id  UUID,
    title_ja        TEXT,
    report_draft    TEXT,
    qa_reply        TEXT,
    resolution      TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
