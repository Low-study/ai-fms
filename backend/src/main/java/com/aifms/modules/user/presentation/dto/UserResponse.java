package com.aifms.modules.user.presentation.dto;

import com.aifms.modules.user.domain.User;

import java.time.Instant;
import java.util.UUID;

/**
 * 用户信息的对外响应体。
 * 绝对不暴露 passwordHash 字段。
 */
public class UserResponse {

    private UUID id;
    private String username;
    private String email;
    private String displayName;
    private String phone;
    private String status;
    private UUID tenantId;
    private Instant lastLoginAt;
    private Instant passwordChangedAt;
    private Instant lockedUntil;
    private int failedLoginCount;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * 从领域对象构建响应体。
     * passwordHash 在此过程中被排除。
     *
     * @param user 领域对象
     * @return 对外响应
     */
    public static UserResponse from(User user) {
        UserResponse r = new UserResponse();
        r.id = user.getId();
        r.username = user.getUsername();
        r.email = user.getEmail();
        r.displayName = user.getDisplayName();
        r.phone = user.getPhone();
        r.status = user.getStatus() != null ? user.getStatus().name() : null;
        r.tenantId = user.getTenantId();
        r.lastLoginAt = user.getLastLoginAt();
        r.passwordChangedAt = user.getPasswordChangedAt();
        r.lockedUntil = user.getLockedUntil();
        r.failedLoginCount = user.getFailedLoginCount();
        r.createdAt = user.getCreatedAt();
        r.updatedAt = user.getUpdatedAt();
        return r;
    }

    // ── Getters & Setters ──

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

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

    public int getFailedLoginCount() { return failedLoginCount; }
    public void setFailedLoginCount(int failedLoginCount) { this.failedLoginCount = failedLoginCount; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
