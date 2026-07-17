package com.aifms.modules.agent.infrastructure;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * {@link IssueEmbeddingEntity} 的 R2DBC 数据访问接口。
 * 支持基于 pgvector 余弦距离的向量相似度检索。
 * 向量写入使用原生 SQL CAST 避免 Java String → pgvector 类型转换失败。
 *
 * @author aifms
 */
@Repository
public interface IssueEmbeddingRepository extends ReactiveCrudRepository<IssueEmbeddingEntity, UUID> {

    /**
     * 按余弦相似度检索与查询向量最相似的嵌入记录。
     * 使用 pgvector 的 {@code <->} 操作符计算余弦距离，结果按距离升序排列。
     *
     * @param queryEmbedding pgvector 格式的查询向量字符串（如 {@code '[0.1,0.2,...]'}）
     * @param limit          返回的最大记录数
     * @return 按相似度降序排列的嵌入实体列表（含 distance 瞬态字段）
     */
    @Query("SELECT ie.id, ie.finding_id, ie.content, ie.model_name, ie.created_at, " +
            "1.0 - (ie.embedding <-> CAST(:queryEmbedding AS vector)) AS similarity " +
            "FROM issue_embeddings ie ORDER BY similarity DESC LIMIT :limit")
    Flux<SimilarEmbeddingRow> findSimilar(String queryEmbedding, int limit);

    /**
     * 原生 SQL 插入向量记录（绕过 R2DBC 的 Java String → pgvector 类型映射限制）。
     */
    @Query("INSERT INTO issue_embeddings (id, finding_id, content, embedding, model_name, created_at) " +
            "VALUES (:id, :findingId, :content, CAST(:embedding AS vector), :modelName, :createdAt)")
    Mono<Integer> insertEmbedding(UUID id, UUID findingId, String content, String embedding, String modelName,
                                   java.time.Instant createdAt);
}
