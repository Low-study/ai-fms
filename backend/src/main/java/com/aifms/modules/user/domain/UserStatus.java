package com.aifms.modules.user.domain;

/**
 * 用户生命周期状态枚举，内置状态机校验。
 * <ul>
 *   <li>ACTIVE   — 正常使用中</li>
 *   <li>LOCKED   — 因安全原因暂时锁定（如密码错误次数过多）</li>
 *   <li>DISABLED — 管理员主动禁用</li>
 *   <li>DELETED  — 软删除，终态，不可逆</li>
 * </ul>
 */
public enum UserStatus {

    ACTIVE,
    LOCKED,
    DISABLED,
    DELETED;

    /**
     * 判断从当前状态转换到目标状态是否合法。
     * DELETED 是终态，不允许转换到任何其他状态。
     *
     * @param target 目标状态
     * @return true 表示允许转换
     */
    public boolean canTransitionTo(UserStatus target) {
        return switch (this) {
            case ACTIVE   -> target == LOCKED || target == DISABLED || target == DELETED;
            case LOCKED   -> target == ACTIVE  || target == DISABLED || target == DELETED;
            case DISABLED -> target == ACTIVE  || target == DELETED;
            case DELETED  -> false;
        };
    }
}
