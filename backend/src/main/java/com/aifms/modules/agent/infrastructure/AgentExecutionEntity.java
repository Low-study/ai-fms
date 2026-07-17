package com.aifms.modules.agent.infrastructure;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * "agent_executions" 表的 R2DBC 持久化实体。
 * 记录 Agent 执行的全生命周期信息。
 *
 * @author aifms
 */
@Table("agent_executions")
public class AgentExecutionEntity {

    /** 主键 */
    @Id
    private UUID id;

    /** Agent 名称 */
    private String agentName;

    /** 输入参数（JSONB） */
    private String inputJson;

    /** 执行状态（QUEUED / RUNNING / SUCCESS / ERROR / CANCELLED） */
    private String status;

    /** 开始时间 */
    private Instant startedAt;

    /** 结束时间 */
    private Instant finishedAt;

    /** 累计 Token 消耗 */
    private Integer tokensUsed;

    /** 累计成本（NUMERIC(10,4)） */
    private BigDecimal cost;

    /** 输出结果（JSONB） */
    private String outputJson;

    /** 错误信息 */
    private String error;

    /** 创建时间 */
    private Instant createdAt;

    // ── Getters & Setters ──

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }

    public String getInputJson() { return inputJson; }
    public void setInputJson(String inputJson) { this.inputJson = inputJson; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }

    public Integer getTokensUsed() { return tokensUsed; }
    public void setTokensUsed(Integer tokensUsed) { this.tokensUsed = tokensUsed; }

    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }

    public String getOutputJson() { return outputJson; }
    public void setOutputJson(String outputJson) { this.outputJson = outputJson; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
