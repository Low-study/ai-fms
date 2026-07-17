package com.aifms.modules.agent.domain;

/**
 * 工单处理完整响应。
 * 聚合了工单从解析到最终输出的全流程结果。
 */
public record IssueResponse(
        /** 原始解析工单 */
        ParsedIssue issue,
        /** 分类标注后的工单 */
        ClassifiedIssue classified,
        /** 相似工单检索结果 */
        SimilarIssues similar,
        /** 生成的报告草稿 */
        String report,
        /** 智能问答回复 */
        String qaReply
) {}
