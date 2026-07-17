package com.aifms.modules.finding.domain;

/**
 * 指摘生命周期状态枚举，内置状态机校验。
 * <ul>
 *   <li>OPEN       — 新建/待处理</li>
 *   <li>ANALYZING  — 分析中</li>
 *   <li>CLASSIFIED — 已分类</li>
 *   <li>RESOLVED   — 已解决</li>
 *   <li>CLOSED     — 已关闭，终态，不可逆</li>
 * </ul>
 *
 * <h3>状态流转</h3>
 * <pre>
 *   OPEN → ANALYZING → CLASSIFIED → RESOLVED → CLOSED
 * </pre>
 * 允许跳过中间状态前向流转，禁止回退（CLOSED 终态除外）。
 */
public enum FindingStatus {

    OPEN,
    ANALYZING,
    CLASSIFIED,
    RESOLVED,
    CLOSED;

    /**
     * 判断从当前状态转换到目标状态是否合法。
     * CLOSED 是终态，不允许转换到任何其他状态。
     * 允许线性前向流转及跳过中间状态（如 OPEN → CLASSIFIED）。
     *
     * @param target 目标状态
     * @return true 表示允许转换
     */
    public boolean canTransitionTo(FindingStatus target) {
        return switch (this) {
            case OPEN       -> target == ANALYZING || target == CLASSIFIED || target == RESOLVED || target == CLOSED;
            case ANALYZING  -> target == CLASSIFIED  || target == RESOLVED   || target == CLOSED;
            case CLASSIFIED -> target == RESOLVED    || target == CLOSED;
            case RESOLVED   -> target == CLOSED;
            case CLOSED     -> false;
        };
    }
}
