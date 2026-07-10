package com.aifms.modules.user.domain;

import com.aifms.common.ErrorCodes;
import com.aifms.common.exception.BusinessException;
import org.springframework.stereotype.Service;

/**
 * 用户领域的跨实体业务规则。
 * 状态机逻辑归 {@link UserStatus} / {@link User} 持有，此处聚焦密码策略等横切规则。
 */
@Service
public class UserDomainService {

    /**
     * 校验原始密码强度。
     * 规则：长度 8~100 字符。
     *
     * @param rawPassword 原始密码
     * @throws BusinessException 40010 密码不符合要求
     */
    public void validatePasswordStrength(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 8) {
            throw new BusinessException(ErrorCodes.PASSWORD_TOO_WEAK, "密码长度不能少于 8 个字符");
        }
        if (rawPassword.length() > 100) {
            throw new BusinessException(ErrorCodes.PASSWORD_TOO_WEAK, "密码长度不能超过 100 个字符");
        }
    }
}
