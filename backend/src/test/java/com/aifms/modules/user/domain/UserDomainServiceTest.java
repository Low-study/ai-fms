package com.aifms.modules.user.domain;

import com.aifms.common.ErrorCodes;
import com.aifms.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户领域服务的密码强度校验测试。
 */
class UserDomainServiceTest {

    private final UserDomainService service = new UserDomainService();

    @Test
    void shouldAcceptPassword_whenMeetsRequirements() {
        assertDoesNotThrow(() -> service.validatePasswordStrength("StrongP@ss1"));
        assertDoesNotThrow(() -> service.validatePasswordStrength("a".repeat(100)));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"short", "1234567"})
    void shouldThrowException_whenPasswordTooShort(String password) {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.validatePasswordStrength(password));
        assertEquals(ErrorCodes.PASSWORD_TOO_WEAK, ex.getCode());
    }

    @Test
    void shouldThrowException_whenPasswordTooLong() {
        String longPassword = "a".repeat(101);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.validatePasswordStrength(longPassword));
        assertEquals(ErrorCodes.PASSWORD_TOO_WEAK, ex.getCode());
    }
}
