package com.aifms.modules.user.presentation;

import com.aifms.common.Result;
import com.aifms.common.dto.PageResult;
import com.aifms.modules.user.application.UserApplicationService;
import com.aifms.modules.user.presentation.dto.CreateUserRequest;
import com.aifms.modules.user.presentation.dto.UpdateUserRequest;
import com.aifms.modules.user.presentation.dto.UserResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 用户管理的 REST 控制器。
 * 职责单一：校验输入 → 调用 Application Service → 返回 Result<T>。
 * 不包含任何业务逻辑。
 */
@Validated
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserApplicationService userApplicationService;

    public UserController(UserApplicationService userApplicationService) {
        this.userApplicationService = userApplicationService;
    }

    /**
     * 分页搜索用户列表。
     *
     * @param keyword 搜索关键字（可选）
     * @param page    页码（默认 1）
     * @param size    每页大小（默认 20）
     */
    @GetMapping
    public Mono<Result<PageResult<UserResponse>>> list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须为正整数") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页大小至少为 1")
                                                @Max(value = 100, message = "每页大小最多为 100") int size) {
        return userApplicationService.listUsers(keyword, page, size);
    }

    /**
     * 按 ID 获取用户详情。
     *
     * @param id 用户 ID
     */
    @GetMapping("/{id}")
    public Mono<Result<UserResponse>> get(@PathVariable UUID id) {
        return userApplicationService.getUser(id);
    }

    /**
     * 创建新用户。
     *
     * @param request 创建请求体（含校验注解）
     */
    @PostMapping
    public Mono<Result<UserResponse>> create(@Valid @RequestBody CreateUserRequest request) {
        return userApplicationService.createUser(request);
    }

    /**
     * 全量更新用户信息（PUT）。
     *
     * @param id      用户 ID
     * @param request 更新请求体
     */
    @PutMapping("/{id}")
    public Mono<Result<UserResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        return userApplicationService.updateUser(id, request);
    }

    /**
     * 部分更新用户信息（PATCH）。
     *
     * @param id      用户 ID
     * @param request 更新请求体（仅更新传入的非空字段）
     */
    @PatchMapping("/{id}")
    public Mono<Result<UserResponse>> patch(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        return userApplicationService.updateUser(id, request);
    }

    /**
     * 软删除用户。
     *
     * @param id 用户 ID
     */
    @DeleteMapping("/{id}")
    public Mono<Result<Void>> delete(@PathVariable UUID id) {
        return userApplicationService.deleteUser(id);
    }
}
