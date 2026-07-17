package com.aifms.modules.file.infrastructure;

import com.aifms.modules.file.domain.UploadedFile;

/**
 * 领域对象 {@link UploadedFile} 与持久化实体 {@link FileEntity} 之间的双向映射器。
 * 所有方法均为纯函数，无状态。
 */
public final class FileMapper {

    private FileMapper() {
        // 工具类，禁止实例化
    }

    /**
     * 将持久化实体转换为纯领域对象。
     *
     * @param entity 数据库实体（可为 null）
     * @return 领域对象，entity 为 null 时返回 null
     */
    public static UploadedFile toDomain(FileEntity entity) {
        if (entity == null) return null;
        UploadedFile file = new UploadedFile();
        file.setId(entity.getId());
        file.setSourceType(entity.getSourceType());
        file.setOriginalName(entity.getOriginalName());
        file.setStoredPath(entity.getStoredPath());
        file.setMime(entity.getMime());
        file.setSize(entity.getSize());
        file.setUploadedBy(entity.getUploadedBy());
        file.setCreatedAt(entity.getCreatedAt());
        return file;
    }

    /**
     * 将领域对象转换为持久化实体。
     *
     * @param file 领域对象（可为 null）
     * @return 数据库实体，file 为 null 时返回 null
     */
    public static FileEntity toEntity(UploadedFile file) {
        if (file == null) return null;
        FileEntity entity = new FileEntity();
        entity.setId(file.getId());
        entity.setSourceType(file.getSourceType());
        entity.setOriginalName(file.getOriginalName());
        entity.setStoredPath(file.getStoredPath());
        entity.setMime(file.getMime());
        entity.setSize(file.getSize());
        entity.setUploadedBy(file.getUploadedBy());
        entity.setCreatedAt(file.getCreatedAt());
        return entity;
    }
}
