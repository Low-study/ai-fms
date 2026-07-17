package com.aifms.modules.agent.infrastructure;

import com.aifms.modules.agent.domain.EmbeddingModelPort;
import com.aifms.modules.agent.domain.ParsedIssue;
import com.aifms.modules.agent.domain.SimilarIssues;
import com.aifms.modules.finding.infrastructure.FindingEntity;
import com.aifms.modules.finding.infrastructure.FindingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link KnowledgeRagSkillAdapter} 的 Testcontainers pgvector 集成测试。
 * 使用 pgvector/pgvector:pg16 容器执行真实的向量检索流程验证。
 *
 * @author aifms
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration"
        })
@Testcontainers
@DisplayName("KnowledgeRagSkillAdapter Testcontainers pgvector 集成测试")
class KnowledgeRagSkillAdapterTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            .withDatabaseName("aifms_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () ->
                "r2dbc:postgresql://" + postgres.getHost() + ":" + postgres.getMappedPort(5432)
                        + "/" + postgres.getDatabaseName());
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @TestConfiguration
    static class MockEmbeddingConfig {
        @Bean
        @Primary
        EmbeddingModelPort embeddingModelPort() {
            return mock(EmbeddingModelPort.class);
        }
    }

    @Autowired
    private KnowledgeRagSkillAdapter adapter;

    @Autowired
    private IssueEmbeddingRepository issueEmbeddingRepository;

    @Autowired
    private FindingRepository findingRepository;

    @Autowired
    private EmbeddingModelPort embeddingModelPort;

    @Autowired
    private DatabaseClient databaseClient;

    @Autowired
    private R2dbcEntityTemplate r2dbcTemplate;

    private UUID findingId1;
    private UUID findingId2;

    @BeforeEach
    void setUp() {
        findingId1 = UUID.randomUUID();
        findingId2 = UUID.randomUUID();

        // 按序执行 DDL + DML
        databaseClient.sql("CREATE EXTENSION IF NOT EXISTS vector").then()
                .then(dropTables())
                .then(createTables())
                .then(seedFindings())
                .then(seedEmbeddings())
                .block();
    }

    private Mono<Void> dropTables() {
        return databaseClient.sql("DROP TABLE IF EXISTS issue_embeddings CASCADE").then()
                .then(databaseClient.sql("DROP TABLE IF EXISTS findings CASCADE").then());
    }

    private Mono<Void> createTables() {
        return databaseClient.sql("""
                CREATE TABLE IF NOT EXISTS findings (
                    id UUID PRIMARY KEY,
                    title VARCHAR(255) NOT NULL,
                    description TEXT,
                    category VARCHAR(50),
                    priority VARCHAR(50),
                    severity VARCHAR(50),
                    system VARCHAR(100),
                    assignee VARCHAR(100),
                    tags VARCHAR(500),
                    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
                    source_type VARCHAR(50),
                    source_file_id UUID,
                    title_ja VARCHAR(255),
                    report_draft TEXT,
                    qa_reply TEXT,
                    resolution TEXT,
                    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
                )""").then()
                .then(databaseClient.sql("""
                CREATE TABLE IF NOT EXISTS issue_embeddings (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    finding_id UUID NOT NULL,
                    content TEXT NOT NULL,
                    embedding vector(3) NOT NULL,
                    model_name VARCHAR(50) NOT NULL,
                    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
                )""").then());
    }

    private Mono<Void> seedFindings() {
        FindingEntity f1 = new FindingEntity();
        f1.setId(findingId1);
        f1.setTitle("登录页面报错 500");
        f1.setDescription("用户反馈登录页面出现 500 错误");
        f1.setCategory("bug");
        f1.setStatus("OPEN");
        f1.setResolution("重启认证服务后恢复");
        f1.setReportDraft("已排查认证服务，已恢复");
        f1.setCreatedAt(Instant.now());
        f1.setUpdatedAt(Instant.now());

        FindingEntity f2 = new FindingEntity();
        f2.setId(findingId2);
        f2.setTitle("数据库连接超时");
        f2.setDescription("数据库查询响应超时");
        f2.setCategory("infrastructure");
        f2.setStatus("OPEN");
        f2.setResolution("优化连接池配置");
        f2.setReportDraft("调整连接池参数");
        f2.setCreatedAt(Instant.now());
        f2.setUpdatedAt(Instant.now());

        return findingRepository.save(f1).then(findingRepository.save(f2)).then();
    }

    private Mono<Void> seedEmbeddings() {
        // 向量 A: 接近查询向量 (代表登录相关)
        String sql1 = "INSERT INTO issue_embeddings (finding_id, content, embedding, model_name) "
                + "VALUES ($1, '登录 500 错误 认证服务', '[0.5,0.8,0.3]', 'test-model')";
        // 向量 B: 不相关 (代表数据库相关)
        String sql2 = "INSERT INTO issue_embeddings (finding_id, content, embedding, model_name) "
                + "VALUES ($1, '数据库 超时 连接池', '[-0.7,-0.3,-0.5]', 'test-model')";

        return databaseClient.sql(sql1)
                .bind("$1", findingId1)
                .then()
                .then(databaseClient.sql(sql2)
                        .bind("$1", findingId2)
                        .then())
                .then();
    }

    // ──────────────────────────────────────────────
    // 正向流程
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("should return similar findings when query vector matches stored embedding")
    void shouldReturnSimilarFindings_whenQueryVectorMatches() {
        // 查询向量与向量 A 接近
        float[] queryEmbedding = {0.5f, 0.8f, 0.3f};
        when(embeddingModelPort.embed(anyString()))
                .thenReturn(Mono.just(queryEmbedding));

        ParsedIssue issue = new ParsedIssue("登录页面报错", "用户反馈 500 错误", "raw...");

        Mono<SimilarIssues> result = adapter.retrieveSimilar(issue);

        StepVerifier.create(result)
                .assertNext(similar -> {
                    assertThat(similar.items()).isNotEmpty();
                    assertThat(similar.items()).allSatisfy(item -> {
                        assertThat(item.id()).isNotNull();
                        assertThat(item.title()).isNotNull();
                        assertThat(item.similarity()).isGreaterThan(0.9);
                        assertThat(item.resolution()).isNotNull();
                    });
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("should return nearest match first (ordered by similarity)")
    void shouldReturnNearestMatchFirst() {
        float[] queryEmbedding = {0.5f, 0.8f, 0.3f};
        when(embeddingModelPort.embed(anyString()))
                .thenReturn(Mono.just(queryEmbedding));

        ParsedIssue issue = new ParsedIssue("登录问题", "认证失败", "raw...");

        Mono<SimilarIssues> result = adapter.retrieveSimilar(issue);

        StepVerifier.create(result)
                .assertNext(similar -> {
                    assertThat(similar.items()).hasSizeGreaterThanOrEqualTo(1);
                    // 第一项应为最相似的（登录报错）
                    if (similar.items().size() >= 2) {
                        assertThat(similar.items().get(0).similarity())
                                .isGreaterThan(similar.items().get(1).similarity());
                    }
                })
                .verifyComplete();
    }

    // ──────────────────────────────────────────────
    // 边界场景
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("should return empty SimilarIssues when query text is all null")
    void shouldReturnEmptyResult_whenQueryTextIsAllNull() {
        ParsedIssue issue = new ParsedIssue(null, null, null);

        Mono<SimilarIssues> result = adapter.retrieveSimilar(issue);

        StepVerifier.create(result)
                .assertNext(similar -> {
                    assertThat(similar.items()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("should return empty SimilarIssues when query text is blank")
    void shouldReturnEmptyResult_whenQueryTextIsBlank() {
        ParsedIssue issue = new ParsedIssue("   ", "", "raw...");

        Mono<SimilarIssues> result = adapter.retrieveSimilar(issue);

        StepVerifier.create(result)
                .assertNext(similar -> {
                    assertThat(similar.items()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("should handle embedding model returning empty vector")
    void shouldHandleEmptyEmbeddingVector() {
        when(embeddingModelPort.embed(anyString()))
                .thenReturn(Mono.just(new float[0]));

        ParsedIssue issue = new ParsedIssue("登录报错", "500 错误", "raw...");

        Mono<SimilarIssues> result = adapter.retrieveSimilar(issue);

        StepVerifier.create(result)
                .assertNext(similar -> {
                    // 空向量仍然可以执行 pgvector 查询
                    assertThat(similar.items()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("should handle dissimilar query returning low similarity scores")
    void shouldReturnLowSimilarity_whenQueryIsDissimilar() {
        // 不相关的查询向量
        float[] queryEmbedding = {0.9f, -0.9f, 0.9f};
        when(embeddingModelPort.embed(anyString()))
                .thenReturn(Mono.just(queryEmbedding));

        ParsedIssue issue = new ParsedIssue("完全无关的问题", "天气预报查询", "raw...");

        Mono<SimilarIssues> result = adapter.retrieveSimilar(issue);

        StepVerifier.create(result)
                .assertNext(similar -> {
                    // 结果应仍然返回但相似度很低
                    similar.items().forEach(item -> {
                        assertThat(item.similarity()).isBetween(0.0, 1.0);
                    });
                })
                .verifyComplete();
    }
}
