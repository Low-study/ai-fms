package com.aifms.modules.file.presentation.dto;

import com.aifms.modules.file.domain.UploadedFile;

import java.time.Instant;
import java.util.UUID;

/**
 * 文件上传的对外响应体。
 * 不含内部存储路径等敏感信息（按需暴露 storedPath）。
 */
public class FileUploadResponse {

    private UUID id;
    private String sourceType;
    private String originalName;
    private String storedPath;
    private String mime;
    private Long size;
    private String uploadedBy;
    private Instant createdAt;

    /**
     * 从领域对象构建响应体。
     *
     * @param file 领域对象
     * @return 对外响应
     */
    public static FileUploadResponse from(UploadedFile file) {
        FileUploadResponse r = new FileUploadResponse();
        r.id = file.getId();
        r.sourceType = file.getSourceType();
        r.originalName = file.getOriginalName();
        r.storedPath = file.getStoredPath();
        r.mime = file.getMime();
        r.size = file.getSize();
        r.uploadedBy = file.getUploadedBy();
        r.createdAt = file.getCreatedAt();
        return r;
    }

    // ── Getters & Setters ──

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public String getStoredPath() { return storedPath; }
    public void setStoredPath(String storedPath) { this.storedPath = storedPath; }

    public String getMime() { return mime; }
    public void setMime(String mime) { this.mime = mime; }

    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }

    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
