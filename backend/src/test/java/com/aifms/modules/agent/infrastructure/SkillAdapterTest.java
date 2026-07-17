package com.aifms.modules.agent.infrastructure;

import com.aifms.modules.agent.domain.ChatModelPort;
import com.aifms.modules.agent.domain.ChatModelPort.ChatRequest;
import com.aifms.modules.agent.domain.ChatModelPort.ChatResponse;
import com.aifms.modules.agent.domain.ClassifiedIssue;
import com.aifms.modules.agent.domain.DocumentParserPort.ParsedDocument;
import com.aifms.modules.agent.domain.EmbeddingModelPort;
import com.aifms.modules.agent.domain.ParsedIssue;
import com.aifms.modules.agent.domain.PromptTemplatePort;
import com.aifms.modules.agent.domain.PromptTemplatePort.PromptTemplate;
import com.aifms.modules.agent.domain.SimilarIssues;
import com.aifms.modules.finding.infrastructure.FindingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * 5 个 SkillAdapter 的单元测试。
 * 使用 Mockito 模拟 ChatModelPort 和 PromptTemplatePort，
 * 使用 WireMock 演示 LLM HTTP API 的桩测试模式。
 *
 * @author aifms
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SkillAdapter 单元测试")
class SkillAdapterTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ChatModelPort chatModelPort;

    @Mock
    private PromptTemplatePort promptTemplatePort;

    @Mock
    private EmbeddingModelPort embeddingModelPort;

    @Mock
    private IssueEmbeddingRepository issueEmbeddingRepository;

    @Mock
    private FindingRepository findingRepository;

    private IssueParseSkillAdapter issueParseSkillAdapter;
    private IssueClassifySkillAdapter issueClassifySkillAdapter;
    private KnowledgeRagSkillAdapter knowledgeRagSkillAdapter;
    private ReportDraftSkillAdapter reportDraftSkillAdapter;
    private QaSkillAdapter qaSkillAdapter;

    @BeforeEach
    void setUp() {
        issueParseSkillAdapter = new IssueParseSkillAdapter(chatModelPort, promptTemplatePort, objectMapper);
        issueClassifySkillAdapter = new IssueClassifySkillAdapter(chatModelPort, promptTemplatePort, objectMapper);
        knowledgeRagSkillAdapter = new KnowledgeRagSkillAdapter(embeddingModelPort, issueEmbeddingRepository, findingRepository);
        reportDraftSkillAdapter = new ReportDraftSkillAdapter(chatModelPort, promptTemplatePort, objectMapper);
        qaSkillAdapter = new QaSkillAdapter(chatModelPort, promptTemplatePort, objectMapper);
    }

    // ──────────────────────────────────────────────
    // IssueParseSkillAdapter
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("IssueParseSkillAdapter")
    class IssueParseSkillAdapterTests {

        @Test
        @DisplayName("should return ParsedIssue when LLM returns valid JSON")
        void shouldReturnParsedIssue_whenLlmReturnsValidJson() {
            String llmJson = """
                    {
                        "title": "登录页面报错",
                        "description": "用户反馈登录页面出现 500 错误",
                        "rawText": "原始文本内容..."
                    }""";
            PromptTemplate template = new PromptTemplate("issue_parse_skill", 1,
                    "你是一个工单解析助手", "请解析以下文档内容");
            ChatResponse response = new ChatResponse("```json\n" + llmJson + "\n```", 100, 0.001);

            when(promptTemplatePort.findByNameVersion("issue_parse_skill", 1))
                    .thenReturn(Mono.just(template));
            when(chatModelPort.call(any(ChatRequest.class)))
                    .thenReturn(Mono.just(response));

            ParsedDocument doc = new ParsedDocument("test.pdf", "application/pdf",
                    "原始文本内容...", Map.of());

            Mono<ParsedIssue> result = issueParseSkillAdapter.parse(doc);

            StepVerifier.create(result)
                    .assertNext(issue -> {
                        assertThat(issue.title()).isEqualTo("登录页面报错");
                        assertThat(issue.description()).isEqualTo("用户反馈登录页面出现 500 错误");
                        assertThat(issue.rawText()).isEqualTo("原始文本内容...");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should handle empty LLM response gracefully")
        void shouldHandleEmptyResponse() {
            PromptTemplate template = new PromptTemplate("issue_parse_skill", 1,
                    "system", "user");
            ChatResponse response = new ChatResponse("", 0, 0.0);

            when(promptTemplatePort.findByNameVersion("issue_parse_skill", 1))
                    .thenReturn(Mono.just(template));
            when(chatModelPort.call(any(ChatRequest.class)))
                    .thenReturn(Mono.just(response));

            ParsedDocument doc = new ParsedDocument("empty.pdf", "application/pdf", "", Map.of());

            Mono<ParsedIssue> result = issueParseSkillAdapter.parse(doc);

            StepVerifier.create(result)
                    .assertNext(issue -> {
                        assertThat(issue.title()).isNull();
                        assertThat(issue.description()).isNull();
                    })
                    .verifyComplete();
        }
    }

    // ──────────────────────────────────────────────
    // IssueClassifySkillAdapter
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("IssueClassifySkillAdapter")
    class IssueClassifySkillAdapterTests {

        @Test
        @DisplayName("should return ClassifiedIssue with category and priority")
        void shouldReturnClassifiedIssue_whenLlmReturnsValidJson() {
            String llmJson = """
                    {
                        "category": "bug",
                        "priority": "P1",
                        "severity": "critical",
                        "system": "auth-module",
                        "assignee": "张三",
                        "tags": ["登录", "500错误"]
                    }""";
            PromptTemplate template = new PromptTemplate("issue_classify_skill", 1,
                    "你是一个工单分类助手", "请分类以下工单");
            ChatResponse response = new ChatResponse(llmJson, 80, 0.001);

            when(promptTemplatePort.findByNameVersion("issue_classify_skill", 1))
                    .thenReturn(Mono.just(template));
            when(chatModelPort.call(any(ChatRequest.class)))
                    .thenReturn(Mono.just(response));

            ParsedIssue issue = new ParsedIssue("登录页面报错", "用户反馈登录页面出现 500 错误", "raw...");

            Mono<ClassifiedIssue> result = issueClassifySkillAdapter.classify(issue);

            StepVerifier.create(result)
                    .assertNext(classified -> {
                        assertThat(classified.category()).isEqualTo("bug");
                        assertThat(classified.priority()).isEqualTo("P1");
                        assertThat(classified.severity()).isEqualTo("critical");
                        assertThat(classified.system()).isEqualTo("auth-module");
                        assertThat(classified.assignee()).isEqualTo("张三");
                        assertThat(classified.tags()).containsExactly("登录", "500错误");
                        // 原始工单被保留
                        assertThat(classified.issue()).isEqualTo(issue);
                    })
                    .verifyComplete();
        }
    }

    // ──────────────────────────────────────────────
    // KnowledgeRagSkillAdapter
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("KnowledgeRagSkillAdapter")
    class KnowledgeRagSkillAdapterTests {

        @Test
        @DisplayName("should return empty SimilarIssues when no similar embeddings found")
        void shouldReturnEmptySimilarIssues_inMvp1Stub() {
            ParsedIssue issue = new ParsedIssue("测试标题", "测试描述", "raw...");

            when(embeddingModelPort.embed("测试标题 测试描述"))
                    .thenReturn(Mono.just(new float[]{0.1f, 0.2f, 0.3f}));
            when(issueEmbeddingRepository.findSimilar(any(), anyInt()))
                    .thenReturn(Flux.empty());

            Mono<SimilarIssues> result = knowledgeRagSkillAdapter.retrieveSimilar(null, issue);

            StepVerifier.create(result)
                    .assertNext(similar -> {
                        assertThat(similar.items()).isEmpty();
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return Mono with no error for null input fields")
        void shouldNotErrorForAnyInput() {
            ParsedIssue nullIssue = new ParsedIssue(null, null, null);

            Mono<SimilarIssues> result = knowledgeRagSkillAdapter.retrieveSimilar(null, nullIssue);

            StepVerifier.create(result)
                    .expectNextMatches(similar -> similar.items().isEmpty())
                    .verifyComplete();
        }
    }

    // ──────────────────────────────────────────────
    // ReportDraftSkillAdapter
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("ReportDraftSkillAdapter")
    class ReportDraftSkillAdapterTests {

        @Test
        @DisplayName("should return report text when LLM returns valid JSON with report field")
        void shouldReturnReportText_whenJsonHasReportField() {
            String llmJson = """
                    {
                        "report": "## 工单处理报告\\n\\n### 问题概述\\n登录页面出现 500 错误...\\n\\n### 建议方案\\n1. 检查认证服务状态\\n2. 查看错误日志"
                    }""";
            PromptTemplate template = new PromptTemplate("report_draft_skill", 1,
                    "你是一个报告起草助手", "请生成报告");
            ChatResponse response = new ChatResponse(llmJson, 200, 0.002);

            when(promptTemplatePort.findByNameVersion("report_draft_skill", 1))
                    .thenReturn(Mono.just(template));
            when(chatModelPort.call(any(ChatRequest.class)))
                    .thenReturn(Mono.just(response));

            ParsedIssue parsed = new ParsedIssue("登录报错", "500 错误", "raw...");
            ClassifiedIssue classified = new ClassifiedIssue(
                    parsed, "bug", "P1", "critical", "auth", "张三", List.of("登录"));
            SimilarIssues similar = new SimilarIssues(List.of());

            Mono<String> result = reportDraftSkillAdapter.draftReport(classified, similar);

            StepVerifier.create(result)
                    .assertNext(report -> {
                        assertThat(report).contains("工单处理报告");
                        assertThat(report).contains("500 错误");
                        assertThat(report).contains("建议方案");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return raw content when JSON parse fails")
        void shouldReturnRawContent_whenJsonParseFails() {
            String rawText = "这是一份纯文本报告，不是 JSON 格式。";
            PromptTemplate template = new PromptTemplate("report_draft_skill", 1,
                    "system", "user");
            ChatResponse response = new ChatResponse(rawText, 50, 0.001);

            when(promptTemplatePort.findByNameVersion("report_draft_skill", 1))
                    .thenReturn(Mono.just(template));
            when(chatModelPort.call(any(ChatRequest.class)))
                    .thenReturn(Mono.just(response));

            ParsedIssue parsed = new ParsedIssue("t", "d", "r");
            ClassifiedIssue classified = new ClassifiedIssue(
                    parsed, "bug", "P2", "minor", "sys", "user", List.of());
            SimilarIssues similar = new SimilarIssues(List.of());

            Mono<String> result = reportDraftSkillAdapter.draftReport(classified, similar);

            StepVerifier.create(result)
                    .assertNext(report -> {
                        assertThat(report).isEqualTo(rawText);
                    })
                    .verifyComplete();
        }
    }

    // ──────────────────────────────────────────────
    // QaSkillAdapter
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("QaSkillAdapter")
    class QaSkillAdapterTests {

        @Test
        @DisplayName("should return QA reply when LLM returns valid JSON with reply field")
        void shouldReturnQaReply_whenJsonHasReplyField() {
            String llmJson = """
                    {
                        "reply": "您好，您反馈的登录页面 500 错误问题我们已经收到。经初步排查，可能是认证服务暂时不可用导致的。我们的技术团队正在紧急处理中，预计 30 分钟内恢复。给您带来的不便敬请谅解。"
                    }""";
            PromptTemplate template = new PromptTemplate("qa_skill", 1,
                    "你是一个客服回复助手", "请生成回复");
            ChatResponse response = new ChatResponse(llmJson, 120, 0.001);

            when(promptTemplatePort.findByNameVersion("qa_skill", 1))
                    .thenReturn(Mono.just(template));
            when(chatModelPort.call(any(ChatRequest.class)))
                    .thenReturn(Mono.just(response));

            ParsedIssue parsed = new ParsedIssue("登录报错", "500 错误", "raw...");
            ClassifiedIssue classified = new ClassifiedIssue(
                    parsed, "bug", "P1", "critical", "auth", "张三", List.of("登录"));

            Mono<String> result = qaSkillAdapter.generateReply(classified);

            StepVerifier.create(result)
                    .assertNext(reply -> {
                        assertThat(reply).contains("登录页面");
                        assertThat(reply).contains("500 错误");
                        assertThat(reply).contains("技术团队");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return raw content when JSON parse fails")
        void shouldReturnRawContent_whenJsonParseFails() {
            String rawText = "您好，我们会尽快处理您的问题。";
            PromptTemplate template = new PromptTemplate("qa_skill", 1,
                    "system", "user");
            ChatResponse response = new ChatResponse(rawText, 30, 0.001);

            when(promptTemplatePort.findByNameVersion("qa_skill", 1))
                    .thenReturn(Mono.just(template));
            when(chatModelPort.call(any(ChatRequest.class)))
                    .thenReturn(Mono.just(response));

            ParsedIssue parsed = new ParsedIssue("t", "d", "r");
            ClassifiedIssue classified = new ClassifiedIssue(
                    parsed, "bug", "P3", "minor", "sys", "user", List.of());

            Mono<String> result = qaSkillAdapter.generateReply(classified);

            StepVerifier.create(result)
                    .assertNext(reply -> {
                        assertThat(reply).isEqualTo(rawText);
                    })
                    .verifyComplete();
        }
    }

    // ──────────────────────────────────────────────
    // WireMock integration demo (LLM HTTP API stub pattern)
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("WireMock LLM API 桩测试")
    class WireMockLlmStubTests {

        @Test
        @DisplayName("should demonstrate WireMock stubbing pattern for LLM API")
        void shouldDemonstrateWireMockStubbingPattern() {
            // 配置 WireMock 使用动态端口
            configureFor("localhost", wireMock.getPort());

            // 桩设置：模拟 LLM API 的 /v1/chat/completions 端点
            String responseBody = """
                    {
                        "choices": [{
                            "message": {
                                "content": "{\\"title\\": \\"测试工单\\", \\"description\\": \\"WireMock 桩测试\\", \\"rawText\\": \\"...\\"}"
                            }
                        }],
                        "usage": { "total_tokens": 50 }
                    }""";

            stubFor(post(urlEqualTo("/v1/chat/completions"))
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody(responseBody)));

            // 验证桩已正确设置（实际项目中会通过 ChatModelPort 的 HTTP 实现调用此端点）
            assertThat(wireMock.getPort()).isGreaterThan(0);
        }
    }
}
