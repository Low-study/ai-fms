package com.aifms.modules.finding.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * 指摘（Finding）领域对象。
 * 不包含任何框架注解，完全与持久化机制解耦。
 * 状态转换逻辑由实体自身持有。
 */
public class Finding {

    /** 指摘唯一标识 */
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

    /** 状态 */
    private FindingStatus status;

    /** 来源类型（如 FILE / MANUAL） */
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

    // ── 构造器 ──

    public Finding() {}

    public Finding(String title, String description, String category, String priority,
                   String severity, String system, String assignee, String tags,
                   String sourceType, UUID sourceFileId, String titleJa) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.priority = priority;
        this.severity = severity;
        this.system = system;
        this.assignee = assignee;
        this.tags = tags;
        this.status = FindingStatus.OPEN;
        this.sourceType = sourceType;
        this.sourceFileId = sourceFileId;
        this.titleJa = titleJa;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // ── 工厂方法 ──

    /**
     * 创建新指摘，初始状态为 OPEN。
     *
     * @param title        指摘标题
     * @param description  描述（可空）
     * @param category     分类（可空）
     * @param priority     优先级（可空）
     * @param severity     严重程度（可空）
     * @param system       所属系统（可空）
     * @param assignee     指派人（可空）
     * @param tags         标签（可空）
     * @param sourceType   来源类型（可空）
     * @param sourceFileId 来源文件 ID（可空）
     * @param titleJa      日文标题（可空）
     * @return 新创建的 Finding 实例
     */
    public static Finding create(String title, String description, String category,
                                 String priority, String severity, String system,
                                 String assignee, String tags, String sourceType,
                                 UUID sourceFileId, String titleJa) {
        return new Finding(title, description, category, priority, severity, system,
                assignee, tags, sourceType, sourceFileId, titleJa);
    }

    // ── 领域行为 ──

    /**
     * 将指摘状态切换到目标状态。
     * 调用前应先通过 {@link FindingStatus#canTransitionTo(FindingStatus)} 校验。
     *
     * @param target 目标状态
     * @throws IllegalStateException 如果状态转换不合法
     */
    public void changeStatus(FindingStatus target) {
        if (!this.status.canTransitionTo(target)) {
            throw new IllegalStateException(
                    "无效的状态转换: " + this.status + " -> " + target);
        }
        this.status = target;
        this.updatedAt = Instant.now();
    }

    /** 关闭指摘：将状态设为 CLOSED 终态（软删除等效操作）。 */
    public void close() {
        changeStatus(FindingStatus.CLOSED);
    }

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

    public FindingStatus getStatus() { return status; }
    public void setStatus(FindingStatus status) { this.status = status; }

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
