-- ============================================
-- V002__create_users.sql — IAM 基础设施：用户表
-- ============================================

-- 确保 pgcrypto 扩展已启用（幂等操作）
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 创建用户表
CREATE TABLE users (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),  -- 主键，自动生成
    username            VARCHAR(50)  NOT NULL,                       -- 用户名（唯一）
    email               VARCHAR(255) NOT NULL,                       -- 邮箱（唯一）
    password_hash       VARCHAR(255) NOT NULL,                       -- 密码哈希值
    display_name        VARCHAR(100),                               -- 显示名称
    phone               VARCHAR(30),                                -- 手机号
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',     -- 状态：ACTIVE/LOCKED/DISABLED/DELETED
    tenant_id           UUID,                                       -- 租户 ID（预留多租户）
    last_login_at       TIMESTAMPTZ,                                -- 最近登录时间
    password_changed_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),        -- 密码最后修改时间
    failed_login_count  INT          NOT NULL DEFAULT 0,            -- 连续登录失败次数
    locked_until        TIMESTAMPTZ,                                -- 锁定解除时间
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),        -- 创建时间
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()         -- 最后更新时间
);

-- 唯一约束（大小写不敏感）
CREATE UNIQUE INDEX uq_users_username ON users (LOWER(username));
CREATE UNIQUE INDEX uq_users_email    ON users (LOWER(email));

-- 查询索引
CREATE INDEX idx_users_status ON users (status);
CREATE INDEX idx_users_tenant ON users (tenant_id);

-- 自动更新 updated_at 字段
CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 状态取值约束
ALTER TABLE users ADD CONSTRAINT ck_users_status
    CHECK (status IN ('ACTIVE', 'LOCKED', 'DISABLED', 'DELETED'));
