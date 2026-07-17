package com.aifms.modules.file.application;

import com.aifms.common.Result;
import com.aifms.modules.file.domain.FileStoragePort;
import com.aifms.modules.file.domain.UploadedFile;
import com.aifms.modules.file.infrastructure.FileMapper;
import com.aifms.modules.file.infrastructure.FileRepository;
import com.aifms.modules.file.presentation.dto.FileUploadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 文件管理的应用服务层。
 * 负责用例编排：接收文件 → 提取元数据 → 存储到 MinIO → 持久化记录 → 返回 DTO。
 * 全链路响应式，禁止使用 {@code .block()}。
 */
@Service
public class FileApplicationService {

    private static final Logger log = LoggerFactory.getLogger(FileApplicationService.class);

    private final FileStoragePort fileStoragePort;
    private final FileRepository fileRepository;
    private final TransactionalOperator transactionalOperator;

    public FileApplicationService(FileStoragePort fileStoragePort,
                                  FileRepository fileRepository,
                                  TransactionalOperator transactionalOperator) {
        this.fileStoragePort = fileStoragePort;
        this.fileRepository = fileRepository;
        this.transactionalOperator = transactionalOperator;
    }

    /**
     * 上传单个文件。
     * 将 multipart 文件流存储到 MinIO 并在数据库中记录元数据。
     *
     * @param filePart Spring WebFlux 的文件部件
     * @return 上传成功的文件响应
     */
    public Mono<Result<FileUploadResponse>> upload(FilePart filePart) {
        String originalName = filePart.filename();
        String contentType = filePart.headers().getContentType() != null
                ? filePart.headers().getContentType().toString()
                : "application/octet-stream";
        String objectKey = generateObjectKey(originalName);

        // 先收集所有 DataBuffer，计算大小后创建新 Flux 用于存储
        // FilePart.content() 只能消费一次，所以先收集再复用
        return filePart.content().collectList()
                .flatMap(buffers -> {
                    long fileSize = buffers.stream()
                            .mapToLong(buf -> buf.readableByteCount())
                            .sum();
                    Flux<DataBuffer> content = Flux.fromIterable(buffers);
                    return fileStoragePort.store(objectKey, content, contentType)
                            .flatMap(storedKey -> {
                                UploadedFile uploadedFile = UploadedFile.create(
                                        null, originalName, storedKey, contentType, fileSize, null);
                                return fileRepository.save(FileMapper.toEntity(uploadedFile));
                            });
                })
                .map(FileMapper::toDomain)
                .map(FileUploadResponse::from)
                .map(Result::success)
                .as(transactionalOperator::transactional)
                .doOnSuccess(result -> log.info("文件上传成功: {} → {}", originalName, objectKey))
                .doOnError(error -> log.error("文件上传失败: {}", originalName, error));
    }

    /**
     * 根据原始文件名生成唯一的对象存储键。
     * 格式：{UUID}/{原始文件名}。
     *
     * @param originalName 原始文件名
     * @return 唯一对象键
     */
    private String generateObjectKey(String originalName) {
        return UUID.randomUUID() + "/" + originalName;
    }
}
