package com.aifms.modules.user.infrastructure;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * "users" 表的 R2DBC 持久化实体。
 * 与 {@link com.aifms.modules.user.domain.User} 平级，通过 {@link UserMapper} 双向转换。
 * 仅包含 getter/setter，不含任何领域逻辑。
 */
@Table("users")
public class UserEntity {

    /** 主键 */
    @Id
    private UUID id;

    /** 用户名（唯一） */
    private String username;

    /** 邮箱（唯一） */
    private String email;

    /** 密码哈希值 */
    private String passwordHash;

    /** 显示名称 */
    private String displayName;

    /** 手机号 */
    private String phone;

    /** 状态（存储为字符串，领域层为枚举） */
    private String status;

    /** 租户 ID */
    private UUID tenantId;

    /** 最近登录时间 */
    private Instant lastLoginAt;

    /** 密码最后修改时间 */
    private Instant passwordChangedAt;

    /** 锁定解除时间 */
    private Instant lockedUntil;

    /** 创建时间 */
    private Instant createdAt;

    /** 最后更新时间 */
    private Instant updatedAt;

    /** 连续登录失败次数 */
    private int failedLoginCount;

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

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

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
