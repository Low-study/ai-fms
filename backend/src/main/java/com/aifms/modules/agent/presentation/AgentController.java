package com.aifms.modules.agent.presentation;

import com.aifms.common.Result;
import com.aifms.modules.agent.application.AgentApplicationService;
import com.aifms.modules.agent.domain.AgentTask;
import com.aifms.modules.file.application.FileApplicationService;
import com.aifms.modules.file.presentation.dto.FileUploadResponse;
import com.aifms.modules.finding.application.FindingApplicationService;
import com.aifms.modules.finding.presentation.dto.CreateFindingRequest;
import com.aifms.modules.finding.presentation.dto.FindingResponse;
import com.aifms.modules.task.domain.TaskProgress;
import com.aifms.modules.task.domain.TaskService;
import com.aifms.modules.task.domain.TaskTicket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

/**
 * Agent 流程的 REST 控制器。
 * 职责单一：接收文件 → 调用 Application Service → 返回 ServerSentEvent 流。
 * 不包含任何业务逻辑。
 *
 * @author aifms
 */
@RestController
@RequestMapping("/api/v1/agents")
public class AgentController {

    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    private final FileApplicationService fileApplicationService;
    private final FindingApplicationService findingApplicationService;
    private final TaskService<Object> taskService;
    private final AgentApplicationService agentApplicationService;

    public AgentController(FileApplicationService fileApplicationService,
                           FindingApplicationService findingApplicationService,
                           TaskService<Object> taskService,
                           AgentApplicationService agentApplicationService) {
        this.fileApplicationService = fileApplicationService;
        this.findingApplicationService = findingApplicationService;
        this.taskService = taskService;
        this.agentApplicationService = agentApplicationService;
    }

    /**
     * 导入工单文件并启动 5-Skill 顺序处理流水线。
     * <p>
     * 端到端流程：
     * <ol>
     *   <li>文件上传 → FileApplicationService.upload(filePart)</li>
     *   <li>创建指摘 → FindingApplicationService.create(OPEN, sourceType=AUTO, sourceFileId)</li>
     *   <li>提交异步任务 → TaskService.submit(AgentTask)</li>
     *   <li>启动 Agent 流水线（异步）→ AgentApplicationService.runSequential(findingId, ticketId)</li>
     *   <li>返回 SSE 进度流 → TaskService.streamProgress(ticketId)</li>
     * </ol>
     *
     * @param filePart 上传的工单文件（multipart/form-data）
     * @return ServerSentEvent 进度流
     */
    @PostMapping(value = "/import", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<TaskProgress<?>>> importIssue(
            @RequestPart("file") FilePart filePart) {

        log.info("收到 Agent 导入请求: filename={}", filePart.filename());

        return fileApplicationService.upload(filePart)
                .map(Result::getData)
                .flatMapMany(fileResp ->
                        createPlaceholderFinding(fileResp)
                                .flatMapMany(finding ->
                                        submitAndRun(finding.getId())
                                )
                );
    }

    /**
     * 创建占位指摘记录（标题为临时占位符，后续由 Skill 流水线更新）。
     */
    private Mono<FindingResponse> createPlaceholderFinding(FileUploadResponse fileResp) {
        CreateFindingRequest createReq = new CreateFindingRequest();
        createReq.setTitle("AUTO-IMPORT-" + System.currentTimeMillis());
        createReq.setSourceType("AUTO");
        createReq.setSourceFileId(fileResp.getId());
        log.info("创建占位指摘: sourceFileId={}", fileResp.getId());
        return findingApplicationService.create(createReq)
                .map(Result::getData);
    }

    /**
     * 提交任务并启动 Agent 流水线，返回 SSE 进度流。
     */
    private Flux<ServerSentEvent<TaskProgress<?>>> submitAndRun(UUID findingId) {
        AgentTask payload = new AgentTask(findingId);
        return taskService.submit(payload)
                .flatMapMany(ticket -> {
                    log.info("任务已提交: ticketId={}, findingId={}", ticket.getTicketId(), findingId);

                    // 异步启动 Agent 流水线（不阻塞 SSE 流返回）
                    agentApplicationService.runSequential(findingId, ticket.getTicketId())
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe(
                                    response -> log.info("Agent 流水线完成: findingId={}, title={}",
                                            findingId, response.issue().title()),
                                    error -> log.error("Agent 流水线失败: findingId={}", findingId, error)
                            );

                    // 返回 SSE 进度流
                    return taskService.streamProgress(ticket.getTicketId())
                            .takeUntil(progress -> "done".equals(progress.getStepName())
                                    || "error".equals(progress.getStepName()))
                            .map(progress -> ServerSentEvent.<TaskProgress<?>>builder()
                                    .id(ticket.getTicketId())
                                    .event(progress.getStepName())
                                    .data(progress)
                                    .build())
                            .concatWithValues(
                                    ServerSentEvent.<TaskProgress<?>>builder()
                                            .event("stream-end")
                                            .comment("Agent pipeline completed")
                                            .build()
                            );
                });
    }
}
