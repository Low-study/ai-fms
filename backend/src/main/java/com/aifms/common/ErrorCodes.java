package com.aifms.common;

/**
 * 错误码常量定义。
 * 所有错误码和消息的权威来源为 {@code resources/error-codes.yml}。
 * 此类仅提供 Java 常量引用，便于代码中类型安全使用。
 *
 * <h3>编码规则</h3>
 * <pre>
 *   4xxxx — 客户端错误
 *     40001-40009  Common（通用）
 *     40010-40029  User（用户模块）
 *     40030-40049  Role（角色模块）
 *     40050-40069  Tenant（租户模块）
 *     40100-40119  Auth（认证模块）
 *     40300-40319  Permission（权限模块）
 *     40401-40419  Resource（资源不存在）
 *   5xxxx — 服务端错误
 *     50001-50009  System（系统异常）
 * </pre>
 */
public final class ErrorCodes {

    private ErrorCodes() { /* 常量类，禁止实例化 */ }

    // ── Common（40001-40009） ──

    /** 参数校验失败（@Valid / ConstraintViolation） */
    public static final int VALIDATION_FAILED = 40001;

    /** 数据库唯一约束冲突（兜底） */
    public static final int DATA_INTEGRITY = 40001;

    // ── User（40010-40029） ──

    /** 用户名已存在 */
    public static final int USERNAME_DUPLICATE = 40002;

    /** 邮箱已存在 */
    public static final int EMAIL_DUPLICATE = 40003;

    /** 密码强度不足 */
    public static final int PASSWORD_TOO_WEAK = 40010;

    /** 不允许的状态变更 */
    public static final int INVALID_STATE_TRANSITION = 40011;

    /** 用户不存在（软删除视为不存在） */
    public static final int USER_NOT_FOUND = 40401;

    // ── System（50001-50009） ──

    /** 服务器内部错误（未预期异常兜底） */
    public static final int INTERNAL_ERROR = 50001;
}
