package com.aifms.modules.finding.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 指摘状态枚举的状态机转换规则测试。
 */
class FindingStatusTest {

    @Test
    void shouldAllowForwardTransitions_fromOpen() {
        assertTrue(FindingStatus.OPEN.canTransitionTo(FindingStatus.ANALYZING));
        assertTrue(FindingStatus.OPEN.canTransitionTo(FindingStatus.CLASSIFIED));
        assertTrue(FindingStatus.OPEN.canTransitionTo(FindingStatus.RESOLVED));
        assertTrue(FindingStatus.OPEN.canTransitionTo(FindingStatus.CLOSED));
        assertFalse(FindingStatus.OPEN.canTransitionTo(FindingStatus.OPEN));
    }

    @Test
    void shouldAllowForwardTransitions_fromAnalyzing() {
        assertTrue(FindingStatus.ANALYZING.canTransitionTo(FindingStatus.CLASSIFIED));
        assertTrue(FindingStatus.ANALYZING.canTransitionTo(FindingStatus.RESOLVED));
        assertTrue(FindingStatus.ANALYZING.canTransitionTo(FindingStatus.CLOSED));
        assertFalse(FindingStatus.ANALYZING.canTransitionTo(FindingStatus.OPEN));
        assertFalse(FindingStatus.ANALYZING.canTransitionTo(FindingStatus.ANALYZING));
    }

    @Test
    void shouldAllowForwardTransitions_fromClassified() {
        assertTrue(FindingStatus.CLASSIFIED.canTransitionTo(FindingStatus.RESOLVED));
        assertTrue(FindingStatus.CLASSIFIED.canTransitionTo(FindingStatus.CLOSED));
        assertFalse(FindingStatus.CLASSIFIED.canTransitionTo(FindingStatus.OPEN));
        assertFalse(FindingStatus.CLASSIFIED.canTransitionTo(FindingStatus.ANALYZING));
    }

    @Test
    void shouldAllowOnlyClosed_fromResolved() {
        assertTrue(FindingStatus.RESOLVED.canTransitionTo(FindingStatus.CLOSED));
        assertFalse(FindingStatus.RESOLVED.canTransitionTo(FindingStatus.OPEN));
        assertFalse(FindingStatus.RESOLVED.canTransitionTo(FindingStatus.ANALYZING));
        assertFalse(FindingStatus.RESOLVED.canTransitionTo(FindingStatus.CLASSIFIED));
    }

    @Test
    void shouldRejectAllTransitions_fromClosed() {
        // CLOSED 是终态，不能转换到任何状态
        assertFalse(FindingStatus.CLOSED.canTransitionTo(FindingStatus.OPEN));
        assertFalse(FindingStatus.CLOSED.canTransitionTo(FindingStatus.ANALYZING));
        assertFalse(FindingStatus.CLOSED.canTransitionTo(FindingStatus.CLASSIFIED));
        assertFalse(FindingStatus.CLOSED.canTransitionTo(FindingStatus.RESOLVED));
        assertFalse(FindingStatus.CLOSED.canTransitionTo(FindingStatus.CLOSED));
    }
}
