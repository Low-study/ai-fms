package com.aifms.modules.user.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户领域对象的领域行为测试。
 */
class UserTest {

    @Test
    void shouldCreateUser_whenFactoryCalled() {
        User user = User.create("john", "john@example.com", "hash", "John", "123456");
        assertEquals("john", user.getUsername());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals(0, user.getFailedLoginCount());
        assertNotNull(user.getPasswordChangedAt());
    }

    @Test
    void shouldChangeStatus_whenValidTransition() {
        User user = User.create("john", "john@example.com", "hash", null, null);
        user.changeStatus(UserStatus.LOCKED);
        assertEquals(UserStatus.LOCKED, user.getStatus());
    }

    @Test
    void shouldThrowException_whenInvalidTransition() {
        User user = User.create("john", "john@example.com", "hash", null, null);
        user.changeStatus(UserStatus.DELETED); // ACTIVE → DELETED 允许
        assertEquals(UserStatus.DELETED, user.getStatus());
        // DELETED → ACTIVE 不允许
        assertThrows(IllegalStateException.class, () -> user.changeStatus(UserStatus.ACTIVE));
    }

    @Test
    void shouldSoftDelete() {
        User user = User.create("john", "john@example.com", "hash", null, null);
        user.softDelete();
        assertEquals(UserStatus.DELETED, user.getStatus());
    }

    @Test
    void shouldRecordLoginSuccess() {
        User user = User.create("john", "john@example.com", "hash", null, null);
        user.setFailedLoginCount(3);
        user.setLockedUntil(java.time.Instant.now());
        user.recordLoginSuccess();
        // 登录成功后：失败计数清零、锁定解除、记录登录时间
        assertEquals(0, user.getFailedLoginCount());
        assertNull(user.getLockedUntil());
        assertNotNull(user.getLastLoginAt());
    }

    @Test
    void shouldRecordLoginFailure_andLockWhenThresholdExceeded() {
        User user = User.create("john", "john@example.com", "hash", null, null);
        // 连续 5 次失败后自动锁定
        for (int i = 0; i < 5; i++) {
            user.recordLoginFailure();
        }
        assertEquals(UserStatus.LOCKED, user.getStatus());
        assertEquals(5, user.getFailedLoginCount());
        assertNotNull(user.getLockedUntil());
    }

    @Test
    void shouldNotBeLocked_whenActive() {
        User user = User.create("john", "john@example.com", "hash", null, null);
        assertFalse(user.isLocked());
    }
}
