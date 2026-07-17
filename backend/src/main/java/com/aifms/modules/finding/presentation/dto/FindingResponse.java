package com.aifms.modules.finding.presentation.dto;

import com.aifms.modules.finding.domain.Finding;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.UUID;

/**
 * 指摘信息的对外响应体。
 */
public class FindingResponse {

    private UUID id;
    private String title;
    private String description;
    private String category;
    private String priority;
    private String severity;
    private String system;
    private String assignee;
    private String tags;
    private String status;
    private String sourceType;
    private UUID sourceFileId;
    private String titleJa;
    private String reportDraft;
    private String qaReply;
    private String resolution;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private Instant createdAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private Instant updatedAt;

    /**
     * 从领域对象构建响应体。
     *
     * @param finding 领域对象
     * @return 对外响应
     */
    public static FindingResponse from(Finding finding) {
        FindingResponse r = new FindingResponse();
        r.id = finding.getId();
        r.title = finding.getTitle();
        r.description = finding.getDescription();
        r.category = finding.getCategory();
        r.priority = finding.getPriority();
        r.severity = finding.getSeverity();
        r.system = finding.getSystem();
        r.assignee = finding.getAssignee();
        r.tags = finding.getTags();
        r.status = finding.getStatus() != null ? finding.getStatus().name() : null;
        r.sourceType = finding.getSourceType();
        r.sourceFileId = finding.getSourceFileId();
        r.titleJa = finding.getTitleJa();
        r.reportDraft = finding.getReportDraft();
        r.qaReply = finding.getQaReply();
        r.resolution = finding.getResolution();
        r.createdAt = finding.getCreatedAt();
        r.updatedAt = finding.getUpdatedAt();
        return r;
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
