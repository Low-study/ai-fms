package com.aifms.modules.user.application;

import com.aifms.common.ErrorCodes;
import com.aifms.common.Result;
import com.aifms.common.dto.PageResult;
import com.aifms.common.exception.BusinessException;
import com.aifms.common.exception.ResourceNotFoundException;
import com.aifms.common.security.PasswordHasher;
import com.aifms.modules.user.domain.User;
import com.aifms.modules.user.domain.UserDomainService;
import com.aifms.modules.user.domain.UserStatus;
import com.aifms.modules.user.infrastructure.UserEntity;
import com.aifms.modules.user.infrastructure.UserRepository;
import com.aifms.modules.user.presentation.dto.CreateUserRequest;
import com.aifms.modules.user.presentation.dto.UpdateUserRequest;
import com.aifms.modules.user.presentation.dto.UserResponse;
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
 * 用户应用服务的用例测试。
 * 模拟 Repository、DomainService、PasswordHasher 和事务管理器。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserApplicationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserDomainService userDomainService;
    @Mock private PasswordHasher passwordHasher;
    @Mock private TransactionalOperator transactionalOperator;

    private UserApplicationService service;
    private UUID userId;
    private UserEntity entity;

    @BeforeEach
    void setUp() {
        service = new UserApplicationService(userRepository, userDomainService, passwordHasher, transactionalOperator);
        userId = UUID.randomUUID();
        entity = new UserEntity();
        entity.setId(userId);
        entity.setUsername("john");
        entity.setEmail("john@example.com");
        entity.setPasswordHash("encoded");
        entity.setStatus("ACTIVE");
        entity.setFailedLoginCount(0);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());

        // 事务管理器模拟：透传 Mono
        when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ── 分页列表 ──

    @Test
    void shouldReturnPaginatedUsers_whenListCalled() {
        when(userRepository.countNonDeleted(null)).thenReturn(Mono.just(1L));
        when(userRepository.findAllNonDeleted(null, 20, 0)).thenReturn(Flux.just(entity));

        StepVerifier.create(service.listUsers("", 1, 20))
                .assertNext(result -> {
                    assertTrue(result.isSuccess());
                    PageResult<UserResponse> page = result.getData();
                    assertEquals(1, page.getTotal());
                    assertEquals(1, page.getItems().size());
                })
                .verifyComplete();
    }

    // ── 按 ID 查询 ──

    @Test
    void shouldReturnUser_whenIdExists() {
        when(userRepository.findByIdNonDeleted(userId)).thenReturn(Mono.just(entity));

        StepVerifier.create(service.getUser(userId))
                .assertNext(result -> {
                    assertTrue(result.isSuccess());
                    assertEquals(UserStatus.ACTIVE.name(), result.getData().getStatus());
                })
                .verifyComplete();
    }

    @Test
    void shouldThrowNotFound_whenIdDoesNotExist() {
        when(userRepository.findByIdNonDeleted(userId)).thenReturn(Mono.empty());

        StepVerifier.create(service.getUser(userId))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    // ── 创建 ──

    @Test
    void shouldCreateUser_whenUsernameAndEmailUnique() {
        CreateUserRequest req = buildCreateRequest();
        when(userRepository.findByUsername("john")).thenReturn(Mono.empty());
        when(userRepository.findByEmail("john@example.com")).thenReturn(Mono.empty());
        when(passwordHasher.encode("password123")).thenReturn("encoded");
        when(userRepository.save(any(UserEntity.class))).thenReturn(Mono.just(entity));

        StepVerifier.create(service.createUser(req))
                .assertNext(result -> {
                    assertTrue(result.isSuccess());
                    assertEquals("john", result.getData().getUsername());
                })
                .verifyComplete();
    }

    @Test
    void shouldThrowException_whenUsernameDuplicate() {
        CreateUserRequest req = buildCreateRequest();
        when(userRepository.findByUsername("john")).thenReturn(Mono.just(entity));
        when(userRepository.findByEmail("john@example.com")).thenReturn(Mono.empty());

        StepVerifier.create(service.createUser(req))
                .expectErrorMatches(ex -> ex instanceof BusinessException e && e.getCode() == ErrorCodes.USERNAME_DUPLICATE)
                .verify();
    }

    @Test
    void shouldThrowException_whenEmailDuplicate() {
        CreateUserRequest req = buildCreateRequest();
        when(userRepository.findByUsername("john")).thenReturn(Mono.empty());
        when(userRepository.findByEmail("john@example.com")).thenReturn(Mono.just(entity));

        StepVerifier.create(service.createUser(req))
                .expectErrorMatches(ex -> ex instanceof BusinessException e && e.getCode() == ErrorCodes.EMAIL_DUPLICATE)
                .verify();
    }

    // ── 更新 ──

    @Test
    void shouldUpdateUser_whenValidData() {
        UpdateUserRequest req = new UpdateUserRequest();
        req.setDisplayName("John Updated");
        when(userRepository.findByIdNonDeleted(userId)).thenReturn(Mono.just(entity));
        when(userRepository.save(any(UserEntity.class))).thenReturn(Mono.just(entity));

        StepVerifier.create(service.updateUser(userId, req))
                .assertNext(result -> assertTrue(result.isSuccess()))
                .verifyComplete();
    }

    // ── 软删除 ──

    @Test
    void shouldSoftDelete_whenUserExists() {
        when(userRepository.findByIdNonDeleted(userId)).thenReturn(Mono.just(entity));
        when(userRepository.save(any(UserEntity.class))).thenReturn(Mono.just(entity));

        StepVerifier.create(service.deleteUser(userId))
                .assertNext(Result::isSuccess)
                .verifyComplete();
    }

    /** 构建测试用创建请求 */
    private CreateUserRequest buildCreateRequest() {
        CreateUserRequest req = new CreateUserRequest();
        req.setUsername("john");
        req.setEmail("john@example.com");
        req.setPassword("password123");
        return req;
    }
}
