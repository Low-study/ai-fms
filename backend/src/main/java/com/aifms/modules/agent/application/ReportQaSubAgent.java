package com.aifms.modules.agent.application;

import com.aifms.modules.agent.domain.ClassifiedIssue;
import com.aifms.modules.agent.domain.ParsedIssue;
import com.aifms.modules.agent.domain.QaSkill;
import com.aifms.modules.agent.domain.ReportDraftSkill;
import com.aifms.modules.agent.domain.SimilarIssueItem;
import com.aifms.modules.agent.domain.SimilarIssues;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 报告起草与智能问答子代理。
 * 组合报告起草（Skill4）和智能问答（Skill5）两个技能，
 * 以 {@link Tool @Tool} 方法暴露给 Supervisor 调用。
 *
 * @author aifms
 */
@Service
public class ReportQaSubAgent {

    private static final Logger log = LoggerFactory.getLogger(ReportQaSubAgent.class);

    private final ReportDraftSkill reportSkill;
    private final QaSkill qaSkill;
    private final ObjectMapper objectMapper;

    public ReportQaSubAgent(ReportDraftSkill reportSkill,
                            QaSkill qaSkill,
                            ObjectMapper objectMapper) {
        this.reportSkill = reportSkill;
        this.qaSkill = qaSkill;
        this.objectMapper = objectMapper;
    }

    /**
     * 基于分类工单和相似工单生成处理报告草稿。
     * 由 Supervisor Agent 作为工具调用。
     *
     * @param ingestResultJson IngestSubAgent 输出的 JSON（包含 title/description/category/priority/severity/system/assignee/tags）
     * @param similarJson      RagSubAgent 输出的相似工单 JSON（可空，MVP-1 阶段为空）
     * @return 报告草稿文本
     */
    @Tool("Generate a processing report draft based on the classified issue and similar historical issues")
    public CompletableFuture<String> draftReport(String ingestResultJson, String similarJson) {
        log.info("ReportQaSubAgent: 开始起草报告");

        return Mono.fromCallable(() -> parseClassifiedIssue(ingestResultJson))
                .flatMap(classified -> {
                    SimilarIssues similar = parseSimilarIssues(similarJson);
                    return reportSkill.draftReport(classified, similar);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .toFuture();
    }

    /**
     * 为分类工单生成智能 QA 回复。
     * 由 Supervisor Agent 作为工具调用。
     *
     * @param ingestResultJson IngestSubAgent 输出的 JSON
     * @return 智能回复文本
     */
    @Tool("Generate an intelligent QA reply for the classified issue")
    public CompletableFuture<String> generateReply(String ingestResultJson) {
        log.info("ReportQaSubAgent: 开始生成QA回复");

        return Mono.fromCallable(() -> parseClassifiedIssue(ingestResultJson))
                .flatMap(qaSkill::generateReply)
                .subscribeOn(Schedulers.boundedElastic())
                .toFuture();
    }

    /**
     * 从 IngestSubAgent 输出的 JSON 重建 ClassifiedIssue。
     */
    private ClassifiedIssue parseClassifiedIssue(String json) {
        if (json == null || json.isBlank()) {
            return new ClassifiedIssue(
                    new ParsedIssue("", "", ""),
                    "", "", "", "", "", List.of());
        }
        try {
            IngestSubAgent.IngestResult r = objectMapper.readValue(json, IngestSubAgent.IngestResult.class);
            ParsedIssue parsed = new ParsedIssue(r.title(), r.description(), r.rawText());
            return new ClassifiedIssue(
                    parsed,
                    r.category() != null ? r.category() : "",
                    r.priority() != null ? r.priority() : "",
                    r.severity() != null ? r.severity() : "",
                    r.system() != null ? r.system() : "",
                    r.assignee() != null ? r.assignee() : "",
                    r.tags() != null ? r.tags() : List.of());
        } catch (JsonProcessingException e) {
            log.error("ReportQaSubAgent: 解析 IngestResult JSON 失败", e);
            return new ClassifiedIssue(
                    new ParsedIssue("", "", ""),
                    "", "", "", "", "", List.of());
        }
    }

    /**
     * 从 JSON 解析 SimilarIssues（容错：解析失败返回空结果）。
     */
    private SimilarIssues parseSimilarIssues(String json) {
        if (json == null || json.isBlank()) {
            return new SimilarIssues(List.of());
        }
        try {
            return objectMapper.readValue(json, SimilarIssues.class);
        } catch (JsonProcessingException e) {
            log.warn("ReportQaSubAgent: 解析 SimilarIssues JSON 失败，返回空列表", e);
            return new SimilarIssues(List.of());
        }
    }

    /**
     * 响应式生成处理报告草稿（供 AgentApplicationService 直接调用）。
     * 全链路响应式，不使用 .block()。
     *
     * @param issue   分类后的工单信息
     * @param similar 相似工单检索结果
     * @return 报告草稿文本
     */
    public Mono<String> draftReportReactive(ClassifiedIssue issue, SimilarIssues similar) {
        log.info("ReportQaSubAgent [reactive]: 开始起草报告, title={}", issue.issue().title());
        return reportSkill.draftReport(issue, similar)
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 响应式生成智能 QA 回复（供 AgentApplicationService 直接调用）。
     * 全链路响应式，不使用 .block()。
     *
     * @param issue 分类后的工单信息
     * @return 智能回复文本
     */
    public Mono<String> generateReplyReactive(ClassifiedIssue issue) {
        log.info("ReportQaSubAgent [reactive]: 开始生成QA回复, title={}", issue.issue().title());
        return qaSkill.generateReply(issue)
                .subscribeOn(Schedulers.boundedElastic());
    }
}
