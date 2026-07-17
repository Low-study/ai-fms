package com.aifms.modules.agent.domain;

import java.util.List;

/**
 * 解析后的工单信息。
 * 从文档解析结果中提取的结构化工单数据。
 */
public record ParsedIssue(
        /** 工单标题 */
        String title,
        /** 工单详细描述 */
        String description,
        /** 原始文本（保留原始格式以供追溯） */
        String rawText
) {}
