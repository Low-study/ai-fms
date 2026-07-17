package com.aifms.modules.finding.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 指摘领域对象的领域行为测试。
 */
class FindingTest {

    @Test
    void shouldCreateFinding_whenFactoryCalled() {
        Finding finding = Finding.create(
                "Test Finding", "Description", "Bug", "High",
                "Critical", "SystemA", "assignee1", "tag1,tag2",
                "FILE", java.util.UUID.randomUUID(), "テスト指摘"
        );
        assertEquals("Test Finding", finding.getTitle());
        assertEquals(FindingStatus.OPEN, finding.getStatus());
        assertNotNull(finding.getCreatedAt());
        assertNotNull(finding.getUpdatedAt());
    }

    @Test
    void shouldAllowValidTransition() {
        Finding finding = Finding.create("Test", null, null, null, null, null, null, null, null, null, null);
        finding.changeStatus(FindingStatus.ANALYZING);
        assertEquals(FindingStatus.ANALYZING, finding.getStatus());
    }

    @Test
    void shouldAllowSkipTransition() {
        Finding finding = Finding.create("Test", null, null, null, null, null, null, null, null, null, null);
        finding.changeStatus(FindingStatus.CLASSIFIED); // OPEN → CLASSIFIED 跳过 ANALYZING
        assertEquals(FindingStatus.CLASSIFIED, finding.getStatus());
    }

    @Test
    void shouldThrowException_whenBackwardTransition() {
        Finding finding = Finding.create("Test", null, null, null, null, null, null, null, null, null, null);
        finding.changeStatus(FindingStatus.ANALYZING);
        // ANALYZING → OPEN 是回退，不允许
        assertThrows(IllegalStateException.class,
                () -> finding.changeStatus(FindingStatus.OPEN));
    }

    @Test
    void shouldThrowException_fromClosed() {
        Finding finding = Finding.create("Test", null, null, null, null, null, null, null, null, null, null);
        finding.close(); // OPEN → CLOSED
        assertEquals(FindingStatus.CLOSED, finding.getStatus());
        assertThrows(IllegalStateException.class,
                () -> finding.changeStatus(FindingStatus.OPEN));
    }

    @Test
    void shouldClose() {
        Finding finding = Finding.create("Test", null, null, null, null, null, null, null, null, null, null);
        finding.close();
        assertEquals(FindingStatus.CLOSED, finding.getStatus());
    }
}
