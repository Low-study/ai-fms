package com.aifms.modules.file.infrastructure;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * "file_uploads" 表的 R2DBC 持久化实体。
 * 与 {@link com.aifms.modules.file.domain.UploadedFile} 平级，通过 {@link FileMapper} 双向转换。
 * 仅包含 getter/setter，不含任何领域逻辑。
 */
@Table("file_uploads")
public class FileEntity {

    /** 主键 */
    @Id
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
