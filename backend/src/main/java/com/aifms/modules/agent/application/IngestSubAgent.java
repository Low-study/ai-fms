package com.aifms.modules.agent.application;

import com.aifms.modules.agent.domain.ClassifiedIssue;
import com.aifms.modules.agent.domain.DocumentParserPort.ParsedDocument;
import com.aifms.modules.agent.domain.IssueClassifySkill;
import com.aifms.modules.agent.domain.IssueParseSkill;
import com.aifms.modules.agent.domain.ParsedIssue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 文档摄入子代理。
 * 组合工单解析（Skill1）和工单分类（Skill2）两个技能，
 * 以 {@link Tool @Tool} 方法暴露给 Supervisor 调用。
 *
 * @author aifms
 */
@Service
public class IngestSubAgent {

    private static final Logger log = LoggerFactory.getLogger(IngestSubAgent.class);

    private final IssueParseSkill parseSkill;
    private final IssueClassifySkill classifySkill;
    private final ObjectMapper objectMapper;

    public IngestSubAgent(IssueParseSkill parseSkill,
                          IssueClassifySkill classifySkill,
                          ObjectMapper objectMapper) {
        this.parseSkill = parseSkill;
        this.classifySkill = classifySkill;
        this.objectMapper = objectMapper;
    }

    /**
     * 解析文档原始文本并分类工单。
     * 由 Supervisor Agent 作为工具调用。
     *
     * @param rawText      文档原始文本内容
     * @param originalName 原始文件名
     * @return JSON 序列化的解析+分类结果，包含 title/description/category/priority/severity/system/assignee/tags
     */
    @Tool("Parse raw document text and classify the issue into structured finding with category, priority, severity, system, assignee, and tags")
    public CompletableFuture<String> parseAndClassify(String rawText, String originalName) {
        log.info("IngestSubAgent: 开始解析并分类文档, originalName={}, textLength={}", originalName,
                rawText != null ? rawText.length() : 0);

        ParsedDocument doc = new ParsedDocument(
                originalName != null ? originalName : "unknown",
                "text/plain",
                rawText != null ? rawText : "",
                Map.of());

        return parseSkill.parse(doc)
                .flatMap(parsed -> {
                    log.info("IngestSubAgent: 解析完成, title={}", parsed.title());
                    return classifySkill.classify(parsed)
                            .map(classified -> {
                                log.info("IngestSubAgent: 分类完成, category={}, priority={}",
                                        classified.category(), classified.priority());
                                return toResultJson(parsed, classified);
                            });
                })
                .subscribeOn(Schedulers.boundedElastic())
                .toFuture();
    }

    /**
     * 将解析和分类结果序列化为 JSON，供 Supervisor LLM 消费。
     */
    private String toResultJson(ParsedIssue parsed, ClassifiedIssue classified) {
        try {
            IngestResult result = new IngestResult(
                    parsed.title(),
                    parsed.description(),
                    parsed.rawText(),
                    classified.category(),
                    classified.priority(),
                    classified.severity(),
                    classified.system(),
                    classified.assignee(),
                    classified.tags()
            );
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            log.error("IngestSubAgent: JSON 序列化失败", e);
            return "{\"error\":\"serialization_failed\"}";
        }
    }

    /**
     * 响应式解析并分类文档（供 AgentApplicationService 直接调用）。
     * 全链路响应式，不使用 .block()。
     *
     * @param document 已解析的文档
     * @return 分类后的工单
     */
    public Mono<ClassifiedIssue> parseAndClassifyReactive(ParsedDocument document) {
        return parseSkill.parse(document)
                .flatMap(parsed -> {
                    log.info("IngestSubAgent [reactive]: 解析完成, title={}", parsed.title());
                    return classifySkill.classify(parsed)
                            .doOnSuccess(classified -> log.info(
                                    "IngestSubAgent [reactive]: 分类完成, category={}, priority={}",
                                    classified.category(), classified.priority()));
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 摄入结果 DTO（仅供 Supervisor LLM 消费）。
     */
    record IngestResult(
            String title,
            String description,
            String rawText,
            String category,
            String priority,
            String severity,
            String system,
            String assignee,
            java.util.List<String> tags
    ) {}
}
