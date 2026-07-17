package com.aifms.modules.agent.infrastructure;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * "issue_embeddings" 表的 R2DBC 持久化实体。
 * embedding 列（vector 类型）不在实体中映射，仅通过原生 @Query 进行向量运算。
 * distance 字段为瞬态，仅用于接收 @Query 中计算的距离值。
 *
 * @author aifms
 */
@Table("issue_embeddings")
public class IssueEmbeddingEntity {

    /** 主键 */
    @Id
    private UUID id;

    /** 关联的指摘记录 ID */
    private UUID findingId;

    /** 嵌入内容（指摘描述文本） */
    private String content;

    /** 嵌入模型名称 */
    private String modelName;

    /** 创建时间 */
    private Instant createdAt;

    /**
     * 余弦距离（瞬态字段，不持久化）。
     * 由原生 @Query 中的 embedding <-> CAST(...) 计算得出。
     */
    @Transient
    private Double distance;

    // ── Getters & Setters ──

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getFindingId() { return findingId; }
    public void setFindingId(UUID findingId) { this.findingId = findingId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Double getDistance() { return distance; }
    public void setDistance(Double distance) { this.distance = distance; }
}
