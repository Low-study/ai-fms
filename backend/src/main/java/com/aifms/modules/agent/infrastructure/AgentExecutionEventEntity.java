package com.aifms.modules.agent.infrastructure;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * "agent_execution_events" 表的 R2DBC 持久化实体。
 * 记录 Agent 执行过程中的每个步骤事件。
 *
 * @author aifms
 */
@Table("agent_execution_events")
public class AgentExecutionEventEntity {

    /** 主键 */
    @Id
    private UUID id;

    /** 所属执行记录 ID */
    private UUID executionId;

    /** 事件类型（如 tool_call、llm_invoke、step_complete） */
    private String eventType;

    /** 步骤名称 */
    private String stepName;

    /** 事件负载（JSONB） */
    private String payloadJson;

    /** 创建时间 */
    private Instant createdAt;

    // ── Getters & Setters ──

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getExecutionId() { return executionId; }
    public void setExecutionId(UUID executionId) { this.executionId = executionId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getStepName() { return stepName; }
    public void setStepName(String stepName) { this.stepName = stepName; }

    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
