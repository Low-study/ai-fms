package com.aifms.modules.finding.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * 创建指摘的请求体。
 */
public class CreateFindingRequest {

    /** 指摘标题 */
    @NotBlank(message = "指摘标题不能为空")
    @Size(max = 500, message = "标题不能超过 500 个字符")
    private String title;

    /** 描述（可选） */
    private String description;

    /** 分类（可选） */
    private String category;

    /** 优先级（可选） */
    private String priority;

    /** 严重程度（可选） */
    private String severity;

    /** 所属系统/模块（可选） */
    private String system;

    /** 指派人（可选） */
    private String assignee;

    /** 标签，逗号分隔（可选） */
    private String tags;

    /** 来源类型（可选） */
    private String sourceType;

    /** 来源文件 ID（可选） */
    private UUID sourceFileId;

    /** 日文标题（可选） */
    private String titleJa;

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
}
