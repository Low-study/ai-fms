package com.aifms.modules.agent.application;

import com.aifms.modules.agent.domain.KnowledgeRagSkill;
import com.aifms.modules.agent.domain.ParsedIssue;
import com.aifms.modules.agent.domain.SimilarIssues;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.CompletableFuture;

/**
 * 知识库 RAG 检索子代理。
 * 封装知识库相似工单检索技能（Skill3），以 {@link Tool @Tool} 方法暴露给 Supervisor 调用。
 *
 * @author aifms
 */
@Service
public class RagSubAgent {

    private static final Logger log = LoggerFactory.getLogger(RagSubAgent.class);

    private final KnowledgeRagSkill ragSkill;
    private final ObjectMapper objectMapper;

    public RagSubAgent(KnowledgeRagSkill ragSkill, ObjectMapper objectMapper) {
        this.ragSkill = ragSkill;
        this.objectMapper = objectMapper;
    }

    /**
     * 基于工单标题和描述从知识库检索相似历史工单。
     * 由 Supervisor Agent 作为工具调用。
     *
     * @param title       工单标题
     * @param description 工单描述
     * @return JSON 序列化的相似工单列表
     */
    @Tool("Retrieve similar historical issues from the knowledge base based on issue title and description")
    public CompletableFuture<String> retrieveSimilar(String title, String description) {
        log.info("RagSubAgent: 开始检索相似工单, title={}", title);

        ParsedIssue issue = new ParsedIssue(
                title != null ? title : "",
                description != null ? description : "",
                "");

        return Mono.defer(() -> ragSkill.retrieveSimilar(issue))
                .map(this::toJson)
                .subscribeOn(Schedulers.boundedElastic())
                .toFuture();
    }

    /**
     * 序列化相似工单结果为 JSON。
     */
    private String toJson(SimilarIssues similar) {
        try {
            return objectMapper.writeValueAsString(similar);
        } catch (JsonProcessingException e) {
            log.error("RagSubAgent: JSON 序列化失败", e);
            return "{\"items\":[]}";
        }
    }

    /**
     * 响应式检索相似历史工单（供 AgentApplicationService 直接调用）。
     * 全链路响应式，不使用 .block()。
     *
     * @param issue 当前工单信息
     * @return 相似工单检索结果
     */
    public Mono<SimilarIssues> retrieveSimilarReactive(ParsedIssue issue) {
        log.info("RagSubAgent [reactive]: 开始检索相似工单, title={}", issue.title());
        return Mono.defer(() -> ragSkill.retrieveSimilar(issue))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
