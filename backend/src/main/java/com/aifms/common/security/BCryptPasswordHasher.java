package com.aifms.common.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * {@link PasswordHasher} 的 BCrypt 实现。
 * 委托给 Spring Security 的 {@link PasswordEncoder} Bean，不直接创建 BCrypt 实例。
 */
@Component
public class BCryptPasswordHasher implements PasswordHasher {

    private final PasswordEncoder delegate;

    /**
     * 通过构造注入 Spring Security 的 PasswordEncoder。
     *
     * @param delegate Spring Security PasswordEncoder 实例
     */
    public BCryptPasswordHasher(PasswordEncoder delegate) {
        this.delegate = delegate;
    }

    @Override
    public String encode(String rawPassword) {
        return delegate.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return delegate.matches(rawPassword, encodedPassword);
    }
}
