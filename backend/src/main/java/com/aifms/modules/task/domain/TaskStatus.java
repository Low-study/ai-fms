package com.aifms.modules.task.domain;

/**
 * 异步任务生命周期状态枚举。
 * <ul>
 *   <li>QUEUED  — 已提交，等待消费</li>
 *   <li>RUNNING — 正在执行</li>
 *   <li>DONE    — 执行成功</li>
 *   <li>FAILED  — 执行失败</li>
 *   <li>ACKED   — 已确认（消费端确认收到）</li>
 * </ul>
 */
public enum TaskStatus {

    QUEUED,
    RUNNING,
    DONE,
    FAILED,
    ACKED;

    /**
     * 判断从当前状态转换到目标状态是否合法。
     *
     * @param target 目标状态
     * @return true 表示允许转换
     */
    public boolean canTransitionTo(TaskStatus target) {
        return switch (this) {
            case QUEUED  -> target == RUNNING || target == FAILED;
            case RUNNING -> target == DONE || target == FAILED;
            case DONE    -> target == ACKED;
            case FAILED  -> target == ACKED;
            case ACKED   -> false;
        };
    }
}
