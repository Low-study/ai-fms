package com.aifms.modules.agent.infrastructure;

import java.time.Instant;
import java.util.UUID;

/**
 * findSimilar 查询结果的简单映射对象（绕过 R2DBC @Transient 别名映射限制）。
 */
public class SimilarEmbeddingRow {

    private UUID id;
    private UUID findingId;
    private String content;
    private String modelName;
    private Instant createdAt;
    private double similarity;

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

    public double getSimilarity() { return similarity; }
    public void setSimilarity(double similarity) { this.similarity = similarity; }
}
