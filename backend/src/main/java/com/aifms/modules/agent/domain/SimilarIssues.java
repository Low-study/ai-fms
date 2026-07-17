package com.aifms.modules.agent.domain;

import java.util.List;

/**
 * 相似工单检索结果。
 * 包含一组与查询工单相似的历史工单项。
 */
public record SimilarIssues(
        /** 相似工单列表 */
        List<SimilarIssueItem> items
) {}
