package com.aifms.modules.user.infrastructure;

import com.aifms.modules.user.domain.User;
import com.aifms.modules.user.domain.UserStatus;

/**
 * 领域对象 {@link User} 与持久化实体 {@link UserEntity} 之间的双向映射器。
 * 所有方法均为纯函数，无状态。
 */
public final class UserMapper {

    private UserMapper() {
        // 工具类，禁止实例化
    }

    /**
     * 将持久化实体转换为纯领域对象。
     *
     * @param entity 数据库实体（可为 null）
     * @return 领域对象，entity 为 null 时返回 null
     */
    public static User toDomain(UserEntity entity) {
        if (entity == null) return null;
        User user = new User();
        user.setId(entity.getId());
        user.setUsername(entity.getUsername());
        user.setEmail(entity.getEmail());
        user.setPasswordHash(entity.getPasswordHash());
        user.setDisplayName(entity.getDisplayName());
        user.setPhone(entity.getPhone());
        user.setStatus(entity.getStatus() != null ? UserStatus.valueOf(entity.getStatus()) : null);
        user.setTenantId(entity.getTenantId());
        user.setLastLoginAt(entity.getLastLoginAt());
        user.setPasswordChangedAt(entity.getPasswordChangedAt());
        user.setLockedUntil(entity.getLockedUntil());
        user.setCreatedAt(entity.getCreatedAt());
        user.setUpdatedAt(entity.getUpdatedAt());
        user.setFailedLoginCount(entity.getFailedLoginCount());
        return user;
    }

    /**
     * 将领域对象转换为持久化实体。
     *
     * @param user 领域对象（可为 null）
     * @return 数据库实体，user 为 null 时返回 null
     */
    public static UserEntity toEntity(User user) {
        if (user == null) return null;
        UserEntity entity = new UserEntity();
        entity.setId(user.getId());
        entity.setUsername(user.getUsername());
        entity.setEmail(user.getEmail());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setDisplayName(user.getDisplayName());
        entity.setPhone(user.getPhone());
        entity.setStatus(user.getStatus() != null ? user.getStatus().name() : null);
        entity.setTenantId(user.getTenantId());
        entity.setLastLoginAt(user.getLastLoginAt());
        entity.setPasswordChangedAt(user.getPasswordChangedAt());
        entity.setLockedUntil(user.getLockedUntil());
        entity.setCreatedAt(user.getCreatedAt());
        entity.setUpdatedAt(user.getUpdatedAt());
        entity.setFailedLoginCount(user.getFailedLoginCount());
        return entity;
    }
}
