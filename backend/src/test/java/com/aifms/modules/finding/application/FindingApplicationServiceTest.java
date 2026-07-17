package com.aifms.modules.finding.application;

import com.aifms.common.ErrorCodes;
import com.aifms.common.Result;
import com.aifms.common.dto.PageResult;
import com.aifms.common.exception.BusinessException;
import com.aifms.modules.finding.domain.FindingDomainService;
import com.aifms.modules.finding.domain.FindingStatus;
import com.aifms.modules.finding.infrastructure.FindingEntity;
import com.aifms.modules.finding.infrastructure.FindingRepository;
import com.aifms.modules.finding.presentation.dto.CreateFindingRequest;
import com.aifms.modules.finding.presentation.dto.FindingResponse;
import com.aifms.modules.finding.presentation.dto.UpdateFindingRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 指摘应用服务的用例测试。
 * 模拟 Repository、DomainService 和事务管理器。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FindingApplicationServiceTest {

    @Mock private FindingRepository findingRepository;
    @Mock private FindingDomainService findingDomainService;
    @Mock private TransactionalOperator transactionalOperator;

    private FindingApplicationService service;
    private UUID findingId;
    private FindingEntity entity;

    @BeforeEach
    void setUp() {
        service = new FindingApplicationService(findingRepository, findingDomainService, transactionalOperator);
        findingId = UUID.randomUUID();
        entity = new FindingEntity();
        entity.setId(findingId);
        entity.setTitle("Test Finding");
        entity.setDescription("A test description");
        entity.setStatus("OPEN");
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());

        // 事务管理器模拟：透传 Mono
        when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ── 分页列表 ──

    @Test
    void shouldReturnPaginatedFindings_whenListCalled() {
        when(findingRepository.countNonClosed(null)).thenReturn(Mono.just(1L));
        when(findingRepository.findAllNonClosed(null, 20, 0)).thenReturn(Flux.just(entity));

        StepVerifier.create(service.listFindings("", 1, 20))
                .assertNext(result -> {
                    assertTrue(result.isSuccess());
                    PageResult<FindingResponse> page = result.getData();
                    assertEquals(1, page.getTotal());
                    assertEquals(1, page.getItems().size());
                })
                .verifyComplete();
    }

    // ── 按 ID 查询 ──

    @Test
    void shouldReturnFinding_whenIdExists() {
        when(findingRepository.findByIdNonClosed(findingId)).thenReturn(Mono.just(entity));

        StepVerifier.create(service.getById(findingId))
                .assertNext(result -> {
                    assertTrue(result.isSuccess());
                    assertEquals("Test Finding", result.getData().getTitle());
                })
                .verifyComplete();
    }

    @Test
    void shouldThrowNotFound_whenIdDoesNotExist() {
        when(findingRepository.findByIdNonClosed(findingId)).thenReturn(Mono.empty());

        StepVerifier.create(service.getById(findingId))
                .expectErrorMatches(ex -> ex instanceof BusinessException e
                        && e.getCode() == ErrorCodes.FINDING_NOT_FOUND)
                .verify();
    }

    // ── 创建 ──

    @Test
    void shouldCreateFinding_whenTitleUnique() {
        CreateFindingRequest req = buildCreateRequest();
        when(findingRepository.findByTitleNonClosed("Test Finding")).thenReturn(Mono.empty());
        when(findingRepository.save(any(FindingEntity.class))).thenReturn(Mono.just(entity));

        StepVerifier.create(service.create(req))
                .assertNext(result -> {
                    assertTrue(result.isSuccess());
                    assertEquals("Test Finding", result.getData().getTitle());
                })
                .verifyComplete();
    }

    @Test
    void shouldThrowException_whenTitleDuplicate() {
        CreateFindingRequest req = buildCreateRequest();
        when(findingRepository.findByTitleNonClosed("Test Finding")).thenReturn(Mono.just(entity));

        StepVerifier.create(service.create(req))
                .expectErrorMatches(ex -> ex instanceof BusinessException e
                        && e.getCode() == ErrorCodes.FINDING_DUPLICATE)
                .verify();
    }

    // ── 更新 ──

    @Test
    void shouldUpdateFinding_whenValidData() {
        UpdateFindingRequest req = new UpdateFindingRequest();
        req.setDescription("Updated description");
        when(findingRepository.findByIdNonClosed(findingId)).thenReturn(Mono.just(entity));
        when(findingRepository.save(any(FindingEntity.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.update(findingId, req))
                .assertNext(result -> assertTrue(result.isSuccess()))
                .verifyComplete();
    }

    @Test
    void shouldChangeStatus_whenValidTransition() {
        UpdateFindingRequest req = new UpdateFindingRequest();
        req.setStatus("ANALYZING");
        when(findingRepository.findByIdNonClosed(findingId)).thenReturn(Mono.just(entity));
        when(findingRepository.save(any(FindingEntity.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.update(findingId, req))
                .assertNext(result -> {
                    assertTrue(result.isSuccess());
                    assertEquals(FindingStatus.ANALYZING.name(), result.getData().getStatus());
                })
                .verifyComplete();
    }

    // ── 软删除 ──

    @Test
    void shouldClose_whenFindingExists() {
        when(findingRepository.findByIdNonClosed(findingId)).thenReturn(Mono.just(entity));
        when(findingRepository.save(any(FindingEntity.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(service.delete(findingId))
                .assertNext(Result::isSuccess)
                .verifyComplete();
    }

    @Test
    void shouldThrowNotFound_whenDeleteNonExistent() {
        when(findingRepository.findByIdNonClosed(findingId)).thenReturn(Mono.empty());

        StepVerifier.create(service.delete(findingId))
                .expectErrorMatches(ex -> ex instanceof BusinessException e
                        && e.getCode() == ErrorCodes.FINDING_NOT_FOUND)
                .verify();
    }

    /** 构建测试用创建请求 */
    private CreateFindingRequest buildCreateRequest() {
        CreateFindingRequest req = new CreateFindingRequest();
        req.setTitle("Test Finding");
        req.setDescription("A test description");
        return req;
    }
}
