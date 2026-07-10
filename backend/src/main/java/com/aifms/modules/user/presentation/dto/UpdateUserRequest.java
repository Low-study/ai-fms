package com.aifms.modules.user.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 更新用户的请求体。
 * 所有字段均为可选——仅传入的字段会被更新。
 */
public class UpdateUserRequest {

    /** 用户名，3-50 字符，仅允许字母、数字、下划线 */
    @Size(min = 3, max = 50, message = "用户名长度需在 3-50 字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    /** 邮箱地址 */
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 新密码（留空表示不修改） */
    @Size(min = 8, max = 100, message = "密码长度需在 8-100 字符之间")
    private String password;

    /** 显示名称 */
    @Size(max = 100, message = "显示名称不能超过 100 个字符")
    private String displayName;

    /** 手机号 */
    @Size(max = 30, message = "手机号不能超过 30 个字符")
    private String phone;

    /** 目标状态（用于 PATCH 变更状态） */
    private String status;

    // ── Getters & Setters ──

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
