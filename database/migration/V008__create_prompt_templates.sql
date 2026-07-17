-- ============================================
-- V008__create_prompt_templates.sql — 版本化 Prompt 模板
-- ============================================
-- V004 约定：数据库只保留 PK + NOT NULL + DEFAULT，
-- 不添加业务索引、UNIQUE 约束、CHECK 约束、触发器。
-- (name, version) 唯一性由后端应用层校验。

CREATE TABLE prompt_templates (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100) NOT NULL,
    version         INT NOT NULL,
    system_template TEXT,
    user_template   TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
