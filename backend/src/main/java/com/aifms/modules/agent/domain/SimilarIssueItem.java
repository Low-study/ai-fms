package com.aifms.modules.agent.domain;

/**
 * 单条相似工单项。
 * 表示一条与当前工单相似的历史工单及其处理信息。
 */
public record SimilarIssueItem(
        /** 历史工单唯一标识 */
        String id,
        /** 历史工单标题 */
        String title,
        /** 相似度得分（0.0~1.0，越高越相似） */
        double similarity,
        /** 历史工单的处理方案 */
        String resolution,
        /** 历史工单的报告草稿 */
        String reportDraft
) {}
