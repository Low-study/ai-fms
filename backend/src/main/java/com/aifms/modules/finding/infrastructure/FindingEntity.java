package com.aifms.modules.finding.infrastructure;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * "findings" 表的 R2DBC 持久化实体。
 * 与 {@link com.aifms.modules.finding.domain.Finding} 平级，通过 {@link FindingMapper} 双向转换。
 * 仅包含 getter/setter，不含任何领域逻辑。
 */
@Table("findings")
public class FindingEntity {

    /** 主键 */
    @Id
    private UUID id;

    /** 标题 */
    private String title;

    /** 描述 */
    private String description;

    /** 分类 */
    private String category;

    /** 优先级 */
    private String priority;

    /** 严重程度 */
    private String severity;

    /** 所属系统/模块 */
    private String system;

    /** 指派人 */
    private String assignee;

    /** 标签（逗号分隔存储） */
    private String tags;

    /** 状态（存储为字符串，领域层为枚举） */
    private String status;

    /** 来源类型 */
    private String sourceType;

    /** 来源文件 ID */
    private UUID sourceFileId;

    /** 日文标题 */
    private String titleJa;

    /** 报告草稿 */
    private String reportDraft;

    /** QA 回复 */
    private String qaReply;

    /** 解决方案 */
    private String resolution;

    /** 创建时间 */
    private Instant createdAt;

    /** 最后更新时间 */
    private Instant updatedAt;

    // ── Getters & Setters ──

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getSystem() { return system; }
    public void setSystem(String system) { this.system = system; }

    public String getAssignee() { return assignee; }
    public void setAssignee(String assignee) { this.assignee = assignee; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public UUID getSourceFileId() { return sourceFileId; }
    public void setSourceFileId(UUID sourceFileId) { this.sourceFileId = sourceFileId; }

    public String getTitleJa() { return titleJa; }
    public void setTitleJa(String titleJa) { this.titleJa = titleJa; }

    public String getReportDraft() { return reportDraft; }
    public void setReportDraft(String reportDraft) { this.reportDraft = reportDraft; }

    public String getQaReply() { return qaReply; }
    public void setQaReply(String qaReply) { this.qaReply = qaReply; }

    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
