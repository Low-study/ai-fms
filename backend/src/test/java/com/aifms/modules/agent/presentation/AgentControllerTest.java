package com.aifms.modules.agent.presentation;

import com.aifms.common.Result;
import com.aifms.modules.agent.application.AgentApplicationService;
import com.aifms.modules.agent.domain.AgentTask;
import com.aifms.modules.agent.domain.ClassifiedIssue;
import com.aifms.modules.agent.domain.IssueResponse;
import com.aifms.modules.agent.domain.ParsedIssue;
import com.aifms.modules.agent.domain.SimilarIssues;
import com.aifms.modules.file.application.FileApplicationService;
import com.aifms.modules.file.presentation.dto.FileUploadResponse;
import com.aifms.modules.finding.application.FindingApplicationService;
import com.aifms.modules.finding.presentation.dto.CreateFindingRequest;
import com.aifms.modules.finding.presentation.dto.FindingResponse;
import com.aifms.modules.task.domain.TaskProgress;
import com.aifms.modules.task.domain.TaskService;
import com.aifms.modules.task.domain.TaskStatus;
import com.aifms.modules.task.domain.TaskTicket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * AgentController 的 WebTestClient 单元测试。
 * 使用 {@link WebFluxTest} 自动配置 WebFlux 基础设施（含 multipart 解析），
 * 通过 {@link MockitoBean} 模拟注入的服务。
 * 验证 SSE 进度流和正常/异常流程。
 *
 * @author aifms
 */
@WebFluxTest(
        controllers = AgentController.class,
        excludeAutoConfiguration = ReactiveSecurityAutoConfiguration.class
)
@AutoConfigureWebTestClient(timeout = "PT15S")
@DisplayName("AgentController WebFlux 集成测试")
class AgentControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private FileApplicationService fileApplicationService;

    @MockitoBean
    private FindingApplicationService findingApplicationService;

    @MockitoBean
    private TaskService<Object> taskService;

    @MockitoBean
    private AgentApplicationService agentApplicationService;

    private static final UUID FILE_ID = UUID.randomUUID();
    private static final UUID FINDING_ID = UUID.randomUUID();
    private static final String TICKET_ID = "test-ticket-abc-123";

    @BeforeEach
    void setUp() {
        // 各测试按需覆盖具体 mock 行为
    }

    // ──────────────────────────────────────────────
    // POST /api/v1/agents/import — 正常流程
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("should return SSE progress stream when import succeeds")
    void shouldReturnSseProgressStream_whenImportSucceeds() {
        arrangeMocksForSuccess();
        when(taskService.streamProgress(TICKET_ID))
                .thenReturn(Flux.just(
                        progress("parse", 20, "工单解析完成"),
                        progress("classify", 40, "分类完成"),
                        progress("rag", 60, "RAG检索完成"),
                        progress("report", 80, "报告起草完成"),
                        progress("qa", 95, "QA回复生成完成"),
                        progress("done", 100, "处理完成")
                ));

        webTestClient.post()
                .uri("/api/v1/agents/import")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(buildMultipartBody())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM);
    }

    @Test
    @DisplayName("should return SSE stream with error events on pipeline failure")
    void shouldReturnSseStream_whenAgentPipelineErrors() {
        arrangeMocksForSuccess();
        when(taskService.streamProgress(TICKET_ID))
                .thenReturn(Flux.just(
                        progress("parse", 20, "工单解析完成"),
                        progress("error", 20, "处理失败: LLM timeout")
                ));

        webTestClient.post()
                .uri("/api/v1/agents/import")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(buildMultipartBody())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM);
    }

    @Test
    @DisplayName("should return SSE stream with only done event for fast pipeline")
    void shouldReturnSseStreamWithOnlyDoneEvent() {
        arrangeMocksForSuccess();
        when(taskService.streamProgress(TICKET_ID))
                .thenReturn(Flux.just(progress("done", 100, "处理完成")));

        webTestClient.post()
                .uri("/api/v1/agents/import")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(buildMultipartBody())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM);
    }

    @Test
    @DisplayName("should handle upload failure gracefully")
    void shouldHandleUploadFailure() {
        when(fileApplicationService.upload(any()))
                .thenReturn(Mono.error(new RuntimeException("文件存储服务不可用")));

        webTestClient.post()
                .uri("/api/v1/agents/import")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(buildMultipartBody())
                .exchange()
                .expectStatus().is5xxServerError();
    }

    // ──────────────────────────────────────────────
    // Helper methods
    // ──────────────────────────────────────────────

    private void arrangeMocksForSuccess() {
        FileUploadResponse fileResp = new FileUploadResponse();
        fileResp.setId(FILE_ID);
        fileResp.setOriginalName("test-issue.pdf");
        fileResp.setStoredPath(FILE_ID + "/test-issue.pdf");
        fileResp.setMime("application/pdf");
        fileResp.setSize(1024L);
        fileResp.setCreatedAt(Instant.now());

        when(fileApplicationService.upload(any()))
                .thenReturn(Mono.just(Result.success(fileResp)));

        FindingResponse findingResp = new FindingResponse();
        findingResp.setId(FINDING_ID);
        findingResp.setTitle("AUTO-IMPORT-placeholder");
        findingResp.setStatus("OPEN");
        findingResp.setSourceType("AUTO");
        findingResp.setSourceFileId(FILE_ID);
        findingResp.setCreatedAt(Instant.now());
        findingResp.setUpdatedAt(Instant.now());

        when(findingApplicationService.create(any(CreateFindingRequest.class)))
                .thenReturn(Mono.just(Result.success(findingResp)));

        TaskTicket ticket = new TaskTicket(TICKET_ID, AgentTask.class.getName(),
                TaskStatus.QUEUED, Instant.now());
        when(taskService.submit(any(AgentTask.class)))
                .thenReturn(Mono.just(ticket));

        ParsedIssue parsed = new ParsedIssue("登录页面报错",
                "用户反馈登录页面出现 500 错误", "原始文本...");
        ClassifiedIssue classified = new ClassifiedIssue(
                parsed, "bug", "P1", "critical",
                "auth-module", "张三", List.of("登录", "500错误"));
        IssueResponse issueResponse = new IssueResponse(parsed, classified,
                new SimilarIssues(List.of()),
                "## 处理报告\n问题已排查...", "您好，您的问题已收到...");

        when(agentApplicationService.runSequential(eq(FINDING_ID), eq(TICKET_ID)))
                .thenReturn(Mono.just(issueResponse));
    }

    private TaskProgress<Object> progress(String stepName, int percentage, String message) {
        return new TaskProgress<>(TICKET_ID, stepName, percentage, message, null);
    }

    private static BodyInserters.MultipartInserter buildMultipartBody() {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", "dummy-pdf-content".getBytes())
                .filename("test-issue.pdf")
                .contentType(MediaType.APPLICATION_PDF);
        return BodyInserters.fromMultipartData(builder.build());
    }
}
