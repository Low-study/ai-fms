package com.aifms.modules.agent.infrastructure;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * {@link IssueEmbeddingEntity} 的 R2DBC 数据访问接口。
 * 支持基于 pgvector 余弦距离的向量相似度检索。
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
    @Query("SELECT ie.*, ie.embedding <-> CAST(:queryEmbedding AS vector) AS distance " +
           "FROM issue_embeddings ie ORDER BY distance LIMIT :limit")
    Flux<IssueEmbeddingEntity> findSimilar(String queryEmbedding, int limit);
}
