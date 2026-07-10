package com.aifms.modules.user.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * 用户领域对象。
 * 不包含任何框架注解，完全与持久化机制解耦。
 * 状态转换逻辑由实体自身持有。
 */
public class User {

    /** 用户唯一标识 */
    private UUID id;

    /** 用户名（唯一） */
    private String username;

    /** 邮箱地址（唯一） */
    private String email;

    /** 密码哈希值（对外不可见） */
    private String passwordHash;

    /** 显示名称 */
    private String displayName;

    /** 手机号 */
    private String phone;

    /** 账户状态 */
    private UserStatus status;

    /** 所属租户 ID（可空，预留多租户） */
    private UUID tenantId;

    /** 最近一次登录时间 */
    private Instant lastLoginAt;

    /** 最近一次密码修改时间 */
    private Instant passwordChangedAt;

    /** 锁定解除时间（null 表示未锁定） */
    private Instant lockedUntil;

    /** 创建时间 */
    private Instant createdAt;

    /** 最后更新时间 */
    private Instant updatedAt;

    /** 连续登录失败次数 */
    private int failedLoginCount;

    /** 连续登录失败最大次数阈值 */
    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;

    /** 账户锁定持续时间（秒），默认 30 分钟 */
    private static final long LOCK_DURATION_SECONDS = 30 * 60;

    // ── 构造器 ──

    public User() {}

    public User(String username, String email, String passwordHash,
                String displayName, String phone) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.phone = phone;
        this.status = UserStatus.ACTIVE;
        this.failedLoginCount = 0;
        this.passwordChangedAt = Instant.now();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // ── 工厂方法 ──

    /**
     * 创建新用户，初始状态为 ACTIVE。
     *
     * @param username     用户名
     * @param email        邮箱
     * @param passwordHash 已哈希的密码
     * @param displayName  显示名称（可空）
     * @param phone        手机号（可空）
     * @return 新创建的 User 实例
     */
    public static User create(String username, String email, String passwordHash,
                              String displayName, String phone) {
        return new User(username, email, passwordHash, displayName, phone);
    }

    // ── 领域行为 ──

    /**
     * 将用户状态切换到目标状态。
     * 调用前应先通过 {@link UserStatus#canTransitionTo(UserStatus)} 校验。
     *
     * @param target 目标状态
     * @throws IllegalStateException 如果状态转换不合法
     */
    public void changeStatus(UserStatus target) {
        if (!this.status.canTransitionTo(target)) {
            throw new IllegalStateException(
                    "无效的状态转换: " + this.status + " -> " + target);
        }
        this.status = target;
        this.updatedAt = Instant.now();
    }

    /** 软删除：将状态设为 DELETED 终态。 */
    public void softDelete() {
        changeStatus(UserStatus.DELETED);
    }

    /**
     * 修改密码。
     *
     * @param newHash 新密码的哈希值
     */
    public void changePassword(String newHash) {
        this.passwordHash = newHash;
        this.passwordChangedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /** 记录一次登录成功：更新时间戳、重置失败计数、清除锁定。 */
    public void recordLoginSuccess() {
        this.lastLoginAt = Instant.now();
        this.failedLoginCount = 0;
        this.lockedUntil = null;
        this.updatedAt = Instant.now();
    }

    /**
     * 记录一次登录失败。
     * 当连续失败次数达到阈值时通过状态机锁定账户。
     */
    public void recordLoginFailure() {
        this.failedLoginCount++;
        this.updatedAt = Instant.now();
        if (this.failedLoginCount >= MAX_FAILED_LOGIN_ATTEMPTS
                && this.status.canTransitionTo(UserStatus.LOCKED)) {
            changeStatus(UserStatus.LOCKED);
            this.lockedUntil = Instant.now().plusSeconds(LOCK_DURATION_SECONDS);
        }
    }

    /**
     * 判断账户当前是否处于锁定状态。
     * 若锁定时间已过期则通过状态机自动解除锁定。
     *
     * @return true 表示当前处于锁定状态
     */
    public boolean isLocked() {
        if (this.status != UserStatus.LOCKED) {
            return false;
        }
        // 锁定时间已过，自动解除
        if (this.lockedUntil != null && Instant.now().isAfter(this.lockedUntil)) {
            changeStatus(UserStatus.ACTIVE);
            this.lockedUntil = null;
            this.failedLoginCount = 0;
            return false;
        }
        return true;
    }

    // ── Getters & Setters ──

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public Instant getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public Instant getPasswordChangedAt() { return passwordChangedAt; }
    public void setPasswordChangedAt(Instant passwordChangedAt) { this.passwordChangedAt = passwordChangedAt; }

    public Instant getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(Instant lockedUntil) { this.lockedUntil = lockedUntil; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public int getFailedLoginCount() { return failedLoginCount; }
    public void setFailedLoginCount(int failedLoginCount) { this.failedLoginCount = failedLoginCount; }
}
