-- V003: 改为部分唯一索引，允许已删除用户的用户名/邮箱被复用
-- 方案 A：软删除后标识符资源可回收

-- 删除旧的全局唯一索引
DROP INDEX IF EXISTS uq_users_username;
DROP INDEX IF EXISTS uq_users_email;

-- 创建部分唯一索引 — 仅对非删除记录强制唯一性
CREATE UNIQUE INDEX uq_users_username ON users (LOWER(username)) WHERE status != 'DELETED';
CREATE UNIQUE INDEX uq_users_email    ON users (LOWER(email))    WHERE status != 'DELETED';
