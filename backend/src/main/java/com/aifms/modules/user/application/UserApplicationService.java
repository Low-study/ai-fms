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
import com.aifms.modules.user.infrastructure.UserMapper;
import com.aifms.modules.user.infrastructure.UserRepository;
import com.aifms.modules.user.presentation.dto.CreateUserRequest;
import com.aifms.modules.user.presentation.dto.UpdateUserRequest;
import com.aifms.modules.user.presentation.dto.UserResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * 用户管理的应用服务层。
 * 负责用例编排：校验 → 领域操作 → 持久化 → 返回 DTO。
 * 全链路响应式，禁止使用 {@code .block()}。
 */
@Service
public class UserApplicationService {

    private final UserRepository userRepository;
    private final UserDomainService userDomainService;
    private final PasswordHasher passwordHasher;
    private final TransactionalOperator transactionalOperator;

    public UserApplicationService(UserRepository userRepository,
                                  UserDomainService userDomainService,
                                  PasswordHasher passwordHasher,
                                  TransactionalOperator transactionalOperator) {
        this.userRepository = userRepository;
        this.userDomainService = userDomainService;
        this.passwordHasher = passwordHasher;
        this.transactionalOperator = transactionalOperator;
    }

    // ── 分页列表 ──

    /**
     * 分页查询非删除用户，支持关键字模糊搜索。
     *
     * @param keyword 搜索关键字（可空）
     * @param page    页码（从 1 开始）
     * @param size    每页大小
     * @return 分页结果
     */
    public Mono<Result<PageResult<UserResponse>>> listUsers(String keyword, int page, int size) {
        String k = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        long offset = (long) (page - 1) * size;

        return userRepository.countNonDeleted(k)
                .flatMap(total -> userRepository.findAllNonDeleted(k, size, offset)
                        .map(UserMapper::toDomain)
                        .map(UserResponse::from)
                        .collectList()
                        .map(items -> {
                            PageResult<UserResponse> pageResult = new PageResult<>(items, page, size, total);
                            return Result.success(pageResult);
                        }));
    }

    // ── 按 ID 查询 ──

    /**
     * 按 ID 查询单个用户。
     *
     * @param id 用户 ID
     * @return 用户响应
     * @throws ResourceNotFoundException 40401 用户不存在或已删除
     */
    public Mono<Result<UserResponse>> getUser(UUID id) {
        return userRepository.findByIdNonDeleted(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User", id)))
                .map(UserMapper::toDomain)
                .map(UserResponse::from)
                .map(Result::success);
    }

    // ── 创建 ──

    /**
     * 创建新用户。
     * 校验用户名和邮箱唯一性，加密密码后持久化。
     *
     * @param request 创建请求体
     * @return 创建成功的用户响应
     * @throws BusinessException 40002 用户名已存在 / 40003 邮箱已存在
     */
    public Mono<Result<UserResponse>> createUser(CreateUserRequest request) {
        userDomainService.validatePasswordStrength(request.getPassword());

        return userRepository.findByUsername(request.getUsername())
                .flatMap(existing -> Mono.<UserResponse>error(
                        new BusinessException(ErrorCodes.USERNAME_DUPLICATE, "用户名已存在: " + request.getUsername())))
                .cast(UserResponse.class)
                .switchIfEmpty(
                        userRepository.findByEmail(request.getEmail())
                                .flatMap(existing -> Mono.<UserResponse>error(
                                        new BusinessException(ErrorCodes.EMAIL_DUPLICATE, "邮箱已存在: " + request.getEmail())))
                                .cast(UserResponse.class)
                )
                .switchIfEmpty(Mono.defer(() -> {
                    String hash = passwordHasher.encode(request.getPassword());
                    User user = User.create(
                            request.getUsername(),
                            request.getEmail(),
                            hash,
                            request.getDisplayName(),
                            request.getPhone()
                    );
                    return userRepository.save(UserMapper.toEntity(user))
                            .map(UserMapper::toDomain)
                            .map(UserResponse::from);
                }))
                .map(Result::success)
                .as(transactionalOperator::transactional);
    }

    // ── 全量更新 ──

    /**
     * 全量更新用户信息。
     * 仅更新传入的非空字段，密码仅在传入时修改。
     *
     * @param id      用户 ID
     * @param request 更新请求体
     * @return 更新后的用户响应
     * @throws ResourceNotFoundException 40401 用户不存在
     * @throws BusinessException         40003 邮箱冲突 / 40001 状态转换非法
     */
    public Mono<Result<UserResponse>> updateUser(UUID id, UpdateUserRequest request) {
        return userRepository.findByIdNonDeleted(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User", id)))
                .map(UserMapper::toDomain)
                .flatMap(user -> {
                    // 用户名唯一性校验（仅在变更时）
                    if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
                        return userRepository.findByUsername(request.getUsername())
                                .flatMap(existing -> Mono.<User>error(
                                        new BusinessException(ErrorCodes.USERNAME_DUPLICATE, "用户名已存在: " + request.getUsername())))
                                .thenReturn(user);
                    }
                    return Mono.just(user);
                })
                .flatMap(user -> {
                    // 邮箱唯一性校验（仅在变更时）
                    if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
                        return userRepository.findByEmail(request.getEmail())
                                .flatMap(existing -> Mono.<User>error(
                                        new BusinessException(ErrorCodes.EMAIL_DUPLICATE, "邮箱已存在: " + request.getEmail())))
                                .thenReturn(user);
                    }
                    return Mono.just(user);
                })
                .flatMap(user -> {
                    if (request.getUsername() != null) user.setUsername(request.getUsername());
                    if (request.getEmail() != null) user.setEmail(request.getEmail());
                    if (request.getDisplayName() != null) user.setDisplayName(request.getDisplayName());
                    if (request.getPhone() != null) user.setPhone(request.getPhone());
                    if (request.getPassword() != null) {
                        userDomainService.validatePasswordStrength(request.getPassword());
                        user.changePassword(passwordHasher.encode(request.getPassword()));
                    }
                    // 状态变更通过状态机校验（目标状态与当前状态相同时跳过）
                    if (request.getStatus() != null) {
                        UserStatus target = UserStatus.valueOf(request.getStatus().toUpperCase());
                        if (target != user.getStatus()) {
                            if (!user.getStatus().canTransitionTo(target)) {
                                return Mono.error(new BusinessException(ErrorCodes.INVALID_STATE_TRANSITION,
                                        "不允许的状态变更: " + user.getStatus() + " -> " + target));
                            }
                            user.changeStatus(target);
                            // 管理员锁定账户时设置锁定到期时间
                            if (target == UserStatus.LOCKED) {
                                user.setLockedUntil(Instant.now().plusSeconds(30 * 60));
                            }
                        }
                    }
                    return userRepository.save(UserMapper.toEntity(user));
                })
                .map(UserMapper::toDomain)
                .map(UserResponse::from)
                .map(Result::success)
                .as(transactionalOperator::transactional);
    }

    // ── 软删除 ──

    /**
     * 软删除用户（将状态设为 DELETED 终态）。
     *
     * @param id 用户 ID
     * @return 空结果
     * @throws ResourceNotFoundException 40401 用户不存在或已删除
     */
    public Mono<Result<Void>> deleteUser(UUID id) {
        return userRepository.findByIdNonDeleted(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User", id)))
                .map(UserMapper::toDomain)
                .flatMap(user -> {
                    user.softDelete();
                    return userRepository.save(UserMapper.toEntity(user));
                })
                .thenReturn(Result.<Void>success())
                .as(transactionalOperator::transactional);
    }
}
