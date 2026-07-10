-- V004: 移除数据库索引、CHECK 约束和触发器
-- 数据库只保留 NOT NULL、DEFAULT、PRIMARY KEY 等基础约束
-- 索引（性能优化）和复杂约束（唯一性、状态校验）交给后端处理

-- 删除唯一索引
DROP INDEX IF EXISTS uq_users_username;
DROP INDEX IF EXISTS uq_users_email;

-- 删除普通索引
DROP INDEX IF EXISTS idx_users_status;
DROP INDEX IF EXISTS idx_users_tenant;

-- 删除 CHECK 约束
ALTER TABLE users DROP CONSTRAINT IF EXISTS ck_users_status;

-- 删除自动更新 updated_at 的触发器
DROP TRIGGER IF EXISTS trg_users_updated_at ON users;

-- 删除触发器函数（如果存在且未被其他表引用）
DROP FUNCTION IF EXISTS update_updated_at_column();
