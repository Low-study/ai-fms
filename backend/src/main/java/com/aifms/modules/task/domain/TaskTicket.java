package com.aifms.modules.task.domain;

import java.time.Instant;

/**
 * 任务凭据 — 提交任务后返回的票据。
 * 持有 ticketId 可查询状态、订阅进度。
 */
public class TaskTicket {

    /** 任务唯一标识 */
    private String ticketId;

    /** 负载类型全限定名 */
    private String payloadType;

    /** 当前任务状态 */
    private TaskStatus status;

    /** 创建时间 */
    private Instant createdAt;

    public TaskTicket() {}

    /**
     * 创建任务凭据。
     *
     * @param ticketId    任务唯一标识
     * @param payloadType 负载类型全限定名
     * @param status      初始状态
     * @param createdAt   创建时间
     */
    public TaskTicket(String ticketId, String payloadType, TaskStatus status, Instant createdAt) {
        this.ticketId = ticketId;
        this.payloadType = payloadType;
        this.status = status;
        this.createdAt = createdAt;
    }

    // ── Getters & Setters ──

    public String getTicketId() { return ticketId; }
    public void setTicketId(String ticketId) { this.ticketId = ticketId; }

    public String getPayloadType() { return payloadType; }
    public void setPayloadType(String payloadType) { this.payloadType = payloadType; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
