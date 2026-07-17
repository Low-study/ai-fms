package com.aifms.modules.file.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * 已上传文件的领域对象。
 * 不包含任何框架注解，完全与持久化机制解耦。
 */
public class UploadedFile {

    /** 文件记录唯一标识 */
    private UUID id;

    /** 来源类型 */
    private String sourceType;

    /** 上传时的原始文件名 */
    private String originalName;

    /** 存储路径（MinIO 对象键） */
    private String storedPath;

    /** MIME 类型 */
    private String mime;

    /** 文件大小（字节） */
    private Long size;

    /** 上传者标识 */
    private String uploadedBy;

    /** 创建时间 */
    private Instant createdAt;

    // ── 构造器 ──

    public UploadedFile() {}

    /**
     * 创建新的文件记录。
     *
     * @param sourceType   来源类型（可空）
     * @param originalName 原始文件名
     * @param storedPath   存储路径
     * @param mime         MIME 类型
     * @param size         文件大小
     * @param uploadedBy   上传者标识（可空）
     */
    public UploadedFile(String sourceType, String originalName, String storedPath,
                        String mime, Long size, String uploadedBy) {
        this.sourceType = sourceType;
        this.originalName = originalName;
        this.storedPath = storedPath;
        this.mime = mime;
        this.size = size;
        this.uploadedBy = uploadedBy;
        this.createdAt = Instant.now();
    }

    // ── 工厂方法 ──

    /**
     * 创建新的上传文件记录。
     *
     * @param sourceType   来源类型（可空）
     * @param originalName 原始文件名
     * @param storedPath   存储路径
     * @param mime         MIME 类型
     * @param size         文件大小
     * @param uploadedBy   上传者标识（可空）
     * @return 新创建的 UploadedFile 实例
     */
    public static UploadedFile create(String sourceType, String originalName, String storedPath,
                                      String mime, Long size, String uploadedBy) {
        return new UploadedFile(sourceType, originalName, storedPath, mime, size, uploadedBy);
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
