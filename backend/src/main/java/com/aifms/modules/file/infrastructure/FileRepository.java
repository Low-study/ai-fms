package com.aifms.modules.file.infrastructure;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * {@link FileEntity} 的 R2DBC 数据访问接口。
 * 提供基本的 CRUD 操作，由 Spring Data R2DBC 自动实现。
 */
@Repository
public interface FileRepository extends ReactiveCrudRepository<FileEntity, UUID> {
}
