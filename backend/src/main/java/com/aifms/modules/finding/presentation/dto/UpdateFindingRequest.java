package com.aifms.modules.finding.presentation.dto;

import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * 更新指摘的请求体。
 * 所有字段均为可选——仅传入的字段会被更新。
 */
public class UpdateFindingRequest {

    /** 指摘标题 */
    @Size(max = 500, message = "标题不能超过 500 个字符")
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

    /** 标签，逗号分隔 */
    private String tags;

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

    /** 目标状态（用于变更状态） */
    private String status;

    // ── Getters & Setters ──

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

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
