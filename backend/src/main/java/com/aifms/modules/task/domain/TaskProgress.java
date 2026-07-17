package com.aifms.modules.task.domain;

/**
 * 任务进度快照。
 * 由执行端通过 {@code publishProgress} 推送，
 * 观察端通过 {@code streamProgress} 订阅。
 *
 * @param <T> 负载数据类型
 */
public class TaskProgress<T> {

    /** 关联的任务凭据 ID */
    private String ticketId;

    /** 当前步骤名称 */
    private String stepName;

    /** 完成百分比 (0-100) */
    private int percentage;

    /** 步骤描述信息 */
    private String message;

    /** 负载数据（可空，用于传递中间结果） */
    private T data;

    /** 消费回执（用于 ack，格式: group:recordId） */
    private String receipt;

    public TaskProgress() {}

    /**
     * 创建任务进度快照。
     *
     * @param ticketId   任务凭据 ID
     * @param stepName   步骤名称
     * @param percentage 完成百分比
     * @param message    描述信息
     * @param data       负载数据（可空）
     */
    public TaskProgress(String ticketId, String stepName, int percentage, String message, T data) {
        this.ticketId = ticketId;
        this.stepName = stepName;
        this.percentage = percentage;
        this.message = message;
        this.data = data;
    }

    // ── Getters & Setters ──

    public String getTicketId() { return ticketId; }
    public void setTicketId(String ticketId) { this.ticketId = ticketId; }

    public String getStepName() { return stepName; }
    public void setStepName(String stepName) { this.stepName = stepName; }

    public int getPercentage() { return percentage; }
    public void setPercentage(int percentage) { this.percentage = percentage; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public String getReceipt() { return receipt; }
    public void setReceipt(String receipt) { this.receipt = receipt; }
}
