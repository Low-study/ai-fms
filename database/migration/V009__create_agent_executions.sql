-- ============================================
-- V009__create_agent_executions.sql — Agent 执行审计日志 + 步骤事件
-- ============================================
-- V004 约定：数据库只保留 PK + NOT NULL + DEFAULT，
-- 不添加业务索引、CHECK 约束、触发器，由后端应用层负责。

CREATE TABLE agent_executions (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_name    VARCHAR(100) NOT NULL,
    input_json    JSONB,
    status        VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    started_at    TIMESTAMPTZ,
    finished_at   TIMESTAMPTZ,
    tokens_used   INT,
    cost          NUMERIC(10,4),
    output_json   JSONB,
    error         TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE agent_execution_events (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    execution_id  UUID NOT NULL,
    event_type    VARCHAR(30) NOT NULL,
    step_name     VARCHAR(50),
    payload_json  JSONB,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
