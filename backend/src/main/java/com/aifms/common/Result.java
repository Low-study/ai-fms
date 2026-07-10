package com.aifms.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Unified API response wrapper.
 * All controller endpoints MUST return Result<T>.
 *
 * @param <T> the type of the data payload
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {

    private int code;
    private String message;
    private T data;
    private long timestamp;

    private Result() {
        this.timestamp = Instant.now().toEpochMilli();
    }

    // ── Success ──

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = 0;
        r.message = "success";
        r.data = data;
        return r;
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    // ── Error ──

    public static <T> Result<T> error(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        return r;
    }

    public static <T> Result<T> error(int code, String message, T data) {
        Result<T> r = error(code, message);
        r.data = data;
        return r;
    }

    // ── Getters ──

    public int getCode() { return code; }
    public String getMessage() { return message; }
    public T getData() { return data; }
    public long getTimestamp() { return timestamp; }

    // ── Convenience ──

    public boolean isSuccess() {
        return code == 0;
    }
}
