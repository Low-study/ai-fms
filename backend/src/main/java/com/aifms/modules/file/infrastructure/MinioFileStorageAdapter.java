package com.aifms.modules.file.infrastructure;

import com.aifms.modules.file.domain.FileStoragePort;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * MinIO 文件存储适配器，实现 {@link FileStoragePort}。
 * MinIO Java SDK 使用阻塞 I/O，所有操作通过
 * {@link Mono#fromCallable} 包装并在 {@link Schedulers#boundedElastic()} 上执行。
 */
@Component
public class MinioFileStorageAdapter implements FileStoragePort {

    private static final Logger log = LoggerFactory.getLogger(MinioFileStorageAdapter.class);

    private final MinioClient minioClient;
    private final String bucketName;

    /**
     * 构造 MinIO 适配器。
     *
     * @param endpoint  MinIO 服务端点
     * @param accessKey 访问密钥
     * @param secretKey 秘密密钥
     * @param bucketName 存储桶名称
     */
    public MinioFileStorageAdapter(
            @Value("${minio.endpoint}") String endpoint,
            @Value("${minio.access-key}") String accessKey,
            @Value("${minio.secret-key}") String secretKey,
            @Value("${minio.bucket-name}") String bucketName) {
        this.bucketName = bucketName;
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        log.info("MinIO 客户端已初始化，端点: {}, 桶: {}", endpoint, bucketName);
    }

    @PostConstruct
    void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("MinIO 桶 '{}' 已自动创建", bucketName);
            }
        } catch (Exception e) {
            log.warn("MinIO 桶 '{}' 自动创建失败: {}", bucketName, e.getMessage());
        }
    }

    @Override
    public Mono<String> store(String key, Flux<DataBuffer> content, String contentType) {
        return content.collectList()
                .flatMap(buffers -> Mono.fromCallable(() -> {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    for (DataBuffer buffer : buffers) {
                        byte[] bytes = new byte[buffer.readableByteCount()];
                        buffer.read(bytes);
                        DataBufferUtils.release(buffer);
                        baos.write(bytes);
                    }
                    byte[] data = baos.toByteArray();
                    ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
                    minioClient.putObject(PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(key)
                            .stream(inputStream, data.length, -1)
                            .contentType(contentType)
                            .build());
                    return key;
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    @Override
    public Flux<DataBuffer> load(String key) {
        return Mono.fromCallable(() -> {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(key)
                    .build()).transferTo(baos);
            byte[] bytes = baos.toByteArray();
            DataBuffer buffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);
            return buffer;
        }).subscribeOn(Schedulers.boundedElastic()).flux();
    }

    @Override
    public Mono<Void> delete(String key) {
        return Mono.fromCallable(() -> {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(key)
                    .build());
            return key;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
