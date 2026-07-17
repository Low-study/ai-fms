package com.aifms.modules.file.application;

import com.aifms.common.Result;
import com.aifms.modules.file.domain.FileStoragePort;
import com.aifms.modules.file.infrastructure.FileEntity;
import com.aifms.modules.file.infrastructure.FileRepository;
import com.aifms.modules.file.presentation.dto.FileUploadResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 文件应用服务的用例测试。
 * 模拟 FileStoragePort、FileRepository 和事务管理器。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FileApplicationServiceTest {

    @Mock
    private FileStoragePort fileStoragePort;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private TransactionalOperator transactionalOperator;

    @Mock
    private FilePart filePart;

    private FileApplicationService service;

    @BeforeEach
    void setUp() {
        service = new FileApplicationService(fileStoragePort, fileRepository, transactionalOperator);

        // 事务管理器模拟：透传 Mono
        when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ── 上传成功 ──

    @Test
    void shouldUploadFile_whenStorageSucceeds() {
        String originalName = "test.txt";
        String contentType = "text/plain";
        byte[] fileBytes = "hello world".getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = DefaultDataBufferFactory.sharedInstance.wrap(fileBytes);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);

        when(filePart.filename()).thenReturn(originalName);
        when(filePart.headers()).thenReturn(headers);
        when(filePart.content()).thenReturn(Flux.just(buffer));

        when(fileStoragePort.store(anyString(), any(), eq(contentType)))
                .thenReturn(Mono.just("stored-key"));
        when(fileRepository.save(any(FileEntity.class)))
                .thenAnswer(inv -> {
                    FileEntity entity = inv.getArgument(0);
                    entity.setId(UUID.randomUUID());
                    entity.setCreatedAt(Instant.now());
                    return Mono.just(entity);
                });

        StepVerifier.create(service.upload(filePart))
                .assertNext(result -> {
                    assertTrue(result.isSuccess());
                    FileUploadResponse resp = result.getData();
                    assertNotNull(resp.getId());
                    assertEquals(originalName, resp.getOriginalName());
                    assertEquals(contentType, resp.getMime());
                    assertNotNull(resp.getCreatedAt());
                })
                .verifyComplete();

        verify(fileStoragePort).store(anyString(), any(), eq(contentType));
        verify(fileRepository).save(any(FileEntity.class));
    }

    // ── 存储失败传播 ──

    @Test
    void shouldPropagateError_whenStorageFails() {
        String originalName = "test.txt";
        byte[] fileBytes = "hello world".getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = DefaultDataBufferFactory.sharedInstance.wrap(fileBytes);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);

        when(filePart.filename()).thenReturn(originalName);
        when(filePart.headers()).thenReturn(headers);
        when(filePart.content()).thenReturn(Flux.just(buffer));

        when(fileStoragePort.store(anyString(), any(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("MinIO 连接失败")));

        StepVerifier.create(service.upload(filePart))
                .expectError(RuntimeException.class)
                .verify();

        verify(fileRepository, never()).save(any());
    }

    // ── 空文件名 + 无 Content-Type ──

    @Test
    void shouldUploadFile_whenEmptyFilenameAndNoContentType() {
        byte[] fileBytes = "data".getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = DefaultDataBufferFactory.sharedInstance.wrap(fileBytes);

        when(filePart.filename()).thenReturn("");
        when(filePart.headers()).thenReturn(new HttpHeaders());
        when(filePart.content()).thenReturn(Flux.just(buffer));

        when(fileStoragePort.store(anyString(), any(), eq("application/octet-stream")))
                .thenReturn(Mono.just("stored-key"));
        when(fileRepository.save(any(FileEntity.class)))
                .thenAnswer(inv -> {
                    FileEntity entity = inv.getArgument(0);
                    entity.setId(UUID.randomUUID());
                    entity.setCreatedAt(Instant.now());
                    return Mono.just(entity);
                });

        StepVerifier.create(service.upload(filePart))
                .assertNext(result -> {
                    assertTrue(result.isSuccess());
                    assertNotNull(result.getData().getId());
                })
                .verifyComplete();
    }
}
