package com.aifms.modules.agent.domain;

import reactor.core.publisher.Mono;
import java.util.UUID;

/**
 * 知识库 RAG 检索技能接口。
 * 基于工单信息从知识库中检索相似的历史工单，辅助生成处理建议。
 *
 * @author aifms
 */
public interface KnowledgeRagSkill {

    /**
     * 检索与当前工单相似的历史工单。
     * MVP-1 阶段返回空结果。
     *
     * @param issue 当前工单信息
     * @return 相似工单检索结果
     */
    Mono<SimilarIssues> retrieveSimilar(UUID findingId, ParsedIssue issue);
}
