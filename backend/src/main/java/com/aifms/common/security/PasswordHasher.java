package com.aifms.common.security;

/**
 * 密码哈希抽象接口。
 * 解耦应用层与具体加密算法（BCrypt、Argon2 等），便于后续替换。
 */
public interface PasswordHasher {

    /**
     * 对原始密码进行哈希。
     *
     * @param rawPassword 明文密码
     * @return 哈希后的密文
     */
    String encode(String rawPassword);

    /**
     * 校验原始密码与存储的哈希是否匹配。
     *
     * @param rawPassword     明文密码
     * @param encodedPassword 已存储的哈希
     * @return true 表示匹配
     */
    boolean matches(String rawPassword, String encodedPassword);
}
