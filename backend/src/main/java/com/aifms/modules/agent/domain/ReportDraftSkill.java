package com.aifms.modules.agent.domain;

import reactor.core.publisher.Mono;

/**
 * 报告起草技能接口。
 * 基于分类工单和相似工单信息生成处理报告草稿。
 *
 * @author aifms
 */
public interface ReportDraftSkill {

    /**
     * 生成工单处理报告草稿。
     *
     * @param issue  分类后的工单信息
     * @param similar 相似工单检索结果
     * @return 报告草稿文本
     */
    Mono<String> draftReport(ClassifiedIssue issue, SimilarIssues similar);
}
