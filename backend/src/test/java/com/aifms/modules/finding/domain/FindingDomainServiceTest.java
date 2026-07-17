package com.aifms.modules.finding.domain;

import com.aifms.common.ErrorCodes;
import com.aifms.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 指摘领域服务的业务规则测试。
 */
class FindingDomainServiceTest {

    private final FindingDomainService service = new FindingDomainService();

    // ── 状态转换校验 ──

    @Test
    void shouldAcceptValidTransition() {
        Finding finding = Finding.create("Test", null, null, null, null, null, null, null, null, null, null);
        assertDoesNotThrow(() -> service.validateStatusTransition(finding, FindingStatus.ANALYZING));
    }

    @Test
    void shouldAcceptSameStatusTransition() {
        Finding finding = Finding.create("Test", null, null, null, null, null, null, null, null, null, null);
        // 相同状态不抛异常
        assertDoesNotThrow(() -> service.validateStatusTransition(finding, FindingStatus.OPEN));
    }

    @Test
    void shouldThrowException_whenBackwardTransition() {
        Finding finding = Finding.create("Test", null, null, null, null, null, null, null, null, null, null);
        finding.changeStatus(FindingStatus.ANALYZING);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.validateStatusTransition(finding, FindingStatus.OPEN));
        assertEquals(ErrorCodes.FINDING_INVALID_STATE, ex.getCode());
    }

    // ── 必填字段校验 ──

    @Test
    void shouldAcceptValidTitle() {
        assertDoesNotThrow(() -> service.validateRequiredFields("Valid Title"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    void shouldThrowException_whenTitleBlank(String title) {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.validateRequiredFields(title));
        assertEquals(ErrorCodes.FINDING_NOT_FOUND, ex.getCode());
    }
}
