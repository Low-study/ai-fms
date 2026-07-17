package com.aifms.modules.agent.application;

import com.aifms.common.Result;
import com.aifms.modules.agent.domain.AgentExecutionLogPort;
import com.aifms.modules.agent.domain.AgentExecutionLogPort.AgentExecution;
import com.aifms.modules.agent.domain.ClassifiedIssue;
import com.aifms.modules.agent.domain.DocumentParserPort;
import com.aifms.modules.agent.domain.DocumentParserPort.ParsedDocument;
import com.aifms.modules.agent.domain.IssueResponse;
import com.aifms.modules.agent.domain.SimilarIssues;
import com.aifms.modules.file.domain.FileStoragePort;
import com.aifms.modules.file.infrastructure.FileEntity;
import com.aifms.modules.file.infrastructure.FileRepository;
import com.aifms.modules.finding.application.FindingApplicationService;
import com.aifms.modules.finding.presentation.dto.FindingResponse;
import com.aifms.modules.finding.presentation.dto.UpdateFindingRequest;
import com.aifms.modules.task.domain.TaskProgress;
import com.aifms.modules.task.domain.TaskService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.UUID;

/**
 * Agent 编排应用服务。
 * 通过 Supervisor 模式协调 3 个子代理（IngestSubAgent、RagSubAgent、ReportQaSubAgent）
 * 执行工单处理流水线：解析+分类 → RAG 检索 → 报告+QA。
 * 每一步通过 TaskService 发布进度，通过 AgentExecutionLogPort 记录执行日志。
 * 全链路响应式，禁止使用 {@code .block()}。
 *
 * @author aifms
 */
@Service
public class AgentApplicationService {

    private static final Logger log = LoggerFactory.getLogger(AgentApplicationService.class);

    private final IngestSubAgent ingestSubAgent;
    private final RagSubAgent ragSubAgent;
    private final ReportQaSubAgent reportQaSubAgent;

    private final FindingApplicationService findingApplicationService;
    private final TaskService<Object> taskService;
    private final AgentExecutionLogPort agentExecutionLogPort;
    private final DocumentParserPort documentParserPort;
    private final FileStoragePort fileStoragePort;
    private final FileRepository fileRepository;
    private final ObjectMapper objectMapper;

    public AgentApplicationService(
            IngestSubAgent ingestSubAgent,
            RagSubAgent ragSubAgent,
            ReportQaSubAgent reportQaSubAgent,
            FindingApplicationService findingApplicationService,
            TaskService<Object> taskService,
            AgentExecutionLogPort agentExecutionLogPort,
            DocumentParserPort documentParserPort,
            FileStoragePort fileStoragePort,
            FileRepository fileRepository,
            ObjectMapper objectMapper) {
        this.ingestSubAgent = ingestSubAgent;
        this.ragSubAgent = ragSubAgent;
        this.reportQaSubAgent = reportQaSubAgent;
        this.findingApplicationService = findingApplicationService;
        this.taskService = taskService;
        this.agentExecutionLogPort = agentExecutionLogPort;
        this.documentParserPort = documentParserPort;
        this.fileStoragePort = fileStoragePort;
        this.fileRepository = fileRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 对指定指摘执行 Supervisor 模式的子代理流水线。
     * <ol>
     *   <li>加载指摘 → 获取来源文件 ID</li>
     *   <li>加载文件内容 → 通过 Tika 解析</li>
     *   <li>IngestSubAgent: 解析 + 分类 → ClassifiedIssue</li>
     *   <li>RagSubAgent: 相似工单检索 → SimilarIssues</li>
     *   <li>ReportQaSubAgent: 报告起草 + QA 回复</li>
     *   <li>更新指摘：分类字段 + 报告草稿 + QA 回复 + status=CLASSIFIED</li>
     * </ol>
     * 每一步通过 TaskService.publishProgress 推送进度，
     * 通过 AgentExecutionLogPort 记录执行事件。
     *
     * @param findingId 指摘 ID
     * @param ticketId  任务凭据 ID（用于进度推送）
     * @return 完整的工单处理响应
     */
    public Mono<IssueResponse> runSequential(UUID findingId, String ticketId) {
        return agentExecutionLogPort.startExecution("supervisor-skill-pipeline",
                        toJson("findingId", findingId, "ticketId", ticketId))
                .flatMap(execution ->
                        loadFindingAndDocument(findingId)
                                .flatMap(doc -> runSubAgentPipeline(doc, ticketId, execution, findingId))
                                .flatMap(response ->
                                        updateFinding(findingId, response)
                                                .flatMap(ignored ->
                                                        agentExecutionLogPort.finishExecution(
                                                                        execution.id(), "success",
                                                                        null, null,
                                                                        toJson("response", response), null)
                                                                .then(publishProgress(ticketId, "done", 100, "处理完成"))
                                                                .thenReturn(response)
                                                )
                                )
                                .onErrorResume(error -> {
                                    log.error("Agent 流水线执行失败: findingId={}", findingId, error);
                                    return agentExecutionLogPort.finishExecution(
                                                    execution.id(), "error",
                                                    null, null, null, error.getMessage())
                                            .then(publishProgress(ticketId, "error", 0, "处理失败: " + error.getMessage()))
                                            .then(Mono.error(error));
                                })
                );
    }

    /**
     * 加载指摘关联的源文件并解析为文档。
     */
    private Mono<ParsedDocument> loadFindingAndDocument(UUID findingId) {
        return findingApplicationService.getById(findingId)
                .map(Result::getData)
                .flatMap(finding -> {
                    UUID sourceFileId = finding.getSourceFileId();
                    if (sourceFileId == null) {
                        return Mono.error(new IllegalStateException("指摘无关联源文件: " + findingId));
                    }
                    return fileRepository.findById(sourceFileId)
                            .switchIfEmpty(Mono.error(
                                    new IllegalStateException("源文件不存在: " + sourceFileId)))
                            .flatMap(fileEntity ->
                                    documentParserPort.parse(
                                            fileStoragePort.load(fileEntity.getStoredPath()))
                            );
                });
    }

    /**
     * 通过 3 个子代理执行流水线，每步发布进度并记录日志。
     */
    private Mono<IssueResponse> runSubAgentPipeline(ParsedDocument doc, String ticketId,
                                                      AgentExecution execution, UUID findingId) {
        log.info("开始执行 SubAgent 流水线: executionId={}", execution.id());

        // SubAgent 1: IngestSubAgent — 解析 + 分类 (0% → 40%)
        return publishProgress(ticketId, "ingest", 0, "IngestSubAgent 开始处理")
                .then(logStep(execution.id(), "subagent_start", "ingest", doc.originalName()))
                .then(ingestSubAgent.parseAndClassifyReactive(doc)
                        .subscribeOn(Schedulers.boundedElastic()))
                .flatMap(classified ->
                        // 先更新 finding 的标题/描述/分类（Ingest 产物），再跑 RAG
                        updateFindingFromIngest(findingId, classified)
                                .then(publishProgress(ticketId, "ingest", 40, "IngestSubAgent 完成"))
                                .then(logStep(execution.id(), "subagent_complete", "ingest",
                                        toJson("classified", classified)))
                                // SubAgent 2: RagSubAgent — 相似工单检索 (40% → 60%)
                                .then(publishProgress(ticketId, "rag", 45, "RagSubAgent 开始检索"))
                                .then(logStep(execution.id(), "subagent_start", "rag", ""))
                                .then(ragSubAgent.retrieveSimilarReactive(findingId, classified.issue())
                                        .subscribeOn(Schedulers.boundedElastic()))
                                .flatMap(similar ->
                                        publishProgress(ticketId, "rag", 60, "RagSubAgent 完成")
                                                .then(logStep(execution.id(), "subagent_complete", "rag",
                                                        toJson("similar", similar)))
                                                // SubAgent 3: ReportQaSubAgent — 报告 + QA (60% → 95%)
                                                .then(publishProgress(ticketId, "report", 65, "ReportQaSubAgent 开始起草报告"))
                                                .then(logStep(execution.id(), "subagent_start", "report", ""))
                                                .then(reportQaSubAgent.draftReportReactive(classified, similar)
                                                        .subscribeOn(Schedulers.boundedElastic()))
                                                .flatMap(report ->
                                                        publishProgress(ticketId, "report", 80, "报告起草完成")
                                                                .then(logStep(execution.id(), "subagent_complete", "report",
                                                                        report.length() + " chars"))
                                                                .then(publishProgress(ticketId, "qa", 85, "ReportQaSubAgent 开始生成QA回复"))
                                                                .then(logStep(execution.id(), "subagent_start", "qa", ""))
                                                                .then(reportQaSubAgent.generateReplyReactive(classified)
                                                                        .subscribeOn(Schedulers.boundedElastic()))
                                                                .flatMap(qaReply ->
                                                                        publishProgress(ticketId, "qa", 95, "QA回复生成完成")
                                                                                .then(logStep(execution.id(), "subagent_complete", "qa",
                                                                                        qaReply.length() + " chars"))
                                                                                .thenReturn(new IssueResponse(
                                                                                        classified.issue(), classified, similar, report, qaReply))
                                                                )
                                                )
                                )
                );
    }

    /**
     * Ingest 完成后先更新 finding 标题/描述/分类，再跑后续 RAG/Report 管道。
     * 这样 RAG 存嵌入时 finding 已有正确的 AI 标题，不会出现 AUTO-IMPORT 残骸。
     */
    private Mono<Void> updateFindingFromIngest(UUID findingId, ClassifiedIssue classified) {
        UpdateFindingRequest request = new UpdateFindingRequest();
        request.setTitle(classified.issue().title());
        request.setDescription(classified.issue().description());
        request.setCategory(classified.category());
        request.setPriority(classified.priority());
        request.setSeverity(classified.severity());
        request.setSystem(classified.system());
        request.setAssignee(classified.assignee());
        request.setTags(classified.tags() != null ? String.join(",", classified.tags()) : null);
        return findingApplicationService.update(findingId, request)
                .doOnSuccess(r -> log.info("Ingest 标题已更新: findingId={}, title={}", findingId, classified.issue().title()))
                .doOnError(e -> log.warn("Ingest 标题更新失败: findingId={}", findingId, e))
                .onErrorResume(e -> Mono.empty()) // 更新失败不阻塞管道
                .then();
    }

    /**
     * 更新指摘记录：填入分类字段 + 报告草稿 + QA 回复 + 状态变更为 CLASSIFIED。
     */
    private Mono<FindingResponse> updateFinding(UUID findingId, IssueResponse response) {
        ClassifiedIssue classified = response.classified();
        UpdateFindingRequest request = new UpdateFindingRequest();
        request.setTitle(classified.issue().title());
        request.setDescription(classified.issue().description());
        request.setCategory(classified.category());
        request.setPriority(classified.priority());
        request.setSeverity(classified.severity());
        request.setSystem(classified.system());
        request.setAssignee(classified.assignee());
        request.setTags(classified.tags() != null ? String.join(",", classified.tags()) : null);
        request.setTitleJa(classified.issue().title()); // MVP: 日文标题同中文
        request.setReportDraft(response.report());
        request.setQaReply(response.qaReply());
        request.setStatus("CLASSIFIED");
        log.info("更新指摘: findingId={}, title={}, status=CLASSIFIED", findingId, classified.issue().title());
        return findingApplicationService.update(findingId, request)
                .map(Result::getData);
    }

    /**
     * 通过 TaskService 发布进度快照。
     */
    private Mono<Void> publishProgress(String ticketId, String stepName, int percentage, String message) {
        TaskProgress<Object> progress = new TaskProgress<>(ticketId, stepName, percentage, message, null);
        return taskService.publishProgress(ticketId, progress)
                .doOnSuccess(v -> log.debug("进度推送: ticketId={}, step={}, {}%", ticketId, stepName, percentage));
    }

    /**
     * 记录 SubAgent 执行步骤事件到 AgentExecutionLogPort。
     */
    private Mono<Void> logStep(UUID executionId, String eventType, String stepName, String payload) {
        return agentExecutionLogPort.appendEvent(executionId, eventType, stepName, payload)
                .doOnSuccess(v -> log.debug("步骤记录: executionId={}, step={}, event={}",
                        executionId, stepName, eventType));
    }

    /**
     * 构建简单的 JSON 键值对字符串。
     */
    private String toJson(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < keyValues.length; i += 2) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(keyValues[i]).append("\":");
            Object value = keyValues[i + 1];
            if (value == null) {
                sb.append("null");
            } else if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else {
                try {
                    sb.append(objectMapper.writeValueAsString(value));
                } catch (JsonProcessingException e) {
                    sb.append("\"").append(value.toString().replace("\"", "\\\"")).append("\"");
                }
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
