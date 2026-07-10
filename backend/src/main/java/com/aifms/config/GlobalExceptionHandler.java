package com.aifms.config;

import com.aifms.common.ErrorCodes;
import com.aifms.common.Result;
import com.aifms.common.exception.BusinessException;
import com.aifms.common.exception.ResourceNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 * 将所有异常转换为统一的 Result&lt;T&gt; 格式响应。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── 参数校验异常 ──

    /** 处理 @Valid 校验失败，汇总所有字段错误 */
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<Result<Void>>> handleValidation(WebExchangeBindException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return Mono.just(ResponseEntity
                .badRequest()
                .body(Result.error(ErrorCodes.VALIDATION_FAILED, "参数校验失败: " + errors)));
    }

    /** 处理方法级校验失败（如 @Min/@Max on @RequestParam） */
    @ExceptionHandler(ConstraintViolationException.class)
    public Mono<ResponseEntity<Result<Void>>> handleConstraintViolation(ConstraintViolationException ex) {
        String errors = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(", "));
        return Mono.just(ResponseEntity
                .badRequest()
                .body(Result.error(ErrorCodes.VALIDATION_FAILED, "参数校验失败: " + errors)));
    }

    // ── 业务异常 ──

    /** 处理业务异常，根据错误码映射 HTTP 状态码 */
    @ExceptionHandler(BusinessException.class)
    public Mono<ResponseEntity<Result<Void>>> handleBusiness(BusinessException ex) {
        log.warn("业务异常: [{}] {}", ex.getCode(), ex.getMessage());
        HttpStatus status = mapCodeToStatus(ex.getCode());
        return Mono.just(ResponseEntity
                .status(status)
                .body(Result.error(ex.getCode(), ex.getMessage())));
    }

    // ── 资源不存在 ──

    @ExceptionHandler(ResourceNotFoundException.class)
    public Mono<ResponseEntity<Result<Void>>> handleNotFound(ResourceNotFoundException ex) {
        return Mono.just(ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Result.error(ex.getCode(), ex.getMessage())));
    }

    // ── 数据库唯一约束冲突 ──

    /**
     * 处理数据库唯一约束冲突（如重复用户名/邮箱）。
     * 根据约束名称精确映射错误码。
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Mono<ResponseEntity<Result<Void>>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String message = ex.getMessage();
        if (message != null && message.contains("uq_users_username")) {
            return Mono.just(ResponseEntity
                    .badRequest()
                    .body(Result.error(ErrorCodes.USERNAME_DUPLICATE, "用户名已存在")));
        }
        if (message != null && message.contains("uq_users_email")) {
            return Mono.just(ResponseEntity
                    .badRequest()
                    .body(Result.error(ErrorCodes.EMAIL_DUPLICATE, "邮箱已存在")));
        }
        log.error("数据完整性异常", ex);
        return Mono.just(ResponseEntity
                .badRequest()
                .body(Result.error(ErrorCodes.DATA_INTEGRITY, "数据完整性冲突")));
    }

    // ── 兜底 ──

    /** 兜底处理所有未捕获异常，返回 500 */
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<Result<Void>>> handleGeneral(Exception ex) {
        log.error("未捕获异常", ex);
        return Mono.just(ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(ErrorCodes.INTERNAL_ERROR, "服务器内部错误")));
    }

    /**
     * 根据业务错误码映射 HTTP 状态码。
     * 4xxxx 系列 → BAD_REQUEST / UNAUTHORIZED / FORBIDDEN / NOT_FOUND。
     */
    private HttpStatus mapCodeToStatus(int code) {
        if (code >= 40100 && code < 40200) return HttpStatus.UNAUTHORIZED;
        if (code >= 40300 && code < 40400) return HttpStatus.FORBIDDEN;
        if (code >= 40400 && code < 40500) return HttpStatus.NOT_FOUND;
        if (code >= 40000 && code < 50000) return HttpStatus.BAD_REQUEST;
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
