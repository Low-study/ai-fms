package com.aifms.modules.user.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户状态枚举的状态机转换规则测试。
 */
class UserStatusTest {

    @Test
    void shouldAllowTransition_fromActive() {
        assertTrue(UserStatus.ACTIVE.canTransitionTo(UserStatus.LOCKED));
        assertTrue(UserStatus.ACTIVE.canTransitionTo(UserStatus.DISABLED));
        assertTrue(UserStatus.ACTIVE.canTransitionTo(UserStatus.DELETED));
        assertFalse(UserStatus.ACTIVE.canTransitionTo(UserStatus.ACTIVE));
    }

    @Test
    void shouldAllowTransition_fromLocked() {
        assertTrue(UserStatus.LOCKED.canTransitionTo(UserStatus.ACTIVE));
        assertTrue(UserStatus.LOCKED.canTransitionTo(UserStatus.DISABLED));
        assertTrue(UserStatus.LOCKED.canTransitionTo(UserStatus.DELETED));
        assertFalse(UserStatus.LOCKED.canTransitionTo(UserStatus.LOCKED));
    }

    @Test
    void shouldAllowTransition_fromDisabled() {
        assertTrue(UserStatus.DISABLED.canTransitionTo(UserStatus.ACTIVE));
        assertTrue(UserStatus.DISABLED.canTransitionTo(UserStatus.DELETED));
        assertFalse(UserStatus.DISABLED.canTransitionTo(UserStatus.LOCKED));
    }

    @Test
    void shouldRejectAllTransitions_fromDeleted() {
        // DELETED 是终态，不能转换到任何状态
        assertFalse(UserStatus.DELETED.canTransitionTo(UserStatus.ACTIVE));
        assertFalse(UserStatus.DELETED.canTransitionTo(UserStatus.LOCKED));
        assertFalse(UserStatus.DELETED.canTransitionTo(UserStatus.DISABLED));
        assertFalse(UserStatus.DELETED.canTransitionTo(UserStatus.DELETED));
    }
}
