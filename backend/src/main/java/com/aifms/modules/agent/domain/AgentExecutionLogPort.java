package com.aifms.modules.agent.domain;

import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Agent 执行日志抽象端口。
 * 记录 Agent 执行的全生命周期日志：启动、步骤事件、完成。
 * 基础设施层负责持久化执行记录（如数据库、日志系统）。
 *
 * @author aifms
 */
public interface AgentExecutionLogPort {

    /**
     * 开始一次 Agent 执行，创建执行记录。
     *
     * @param agentName Agent 名称
     * @param inputJson 输入参数（JSON 格式）
     * @return 新建的执行记录（含自动生成的唯一标识）
     */
    Mono<AgentExecution> startExecution(String agentName, String inputJson);

    /**
     * 在执行记录中追加一个步骤事件。
     *
     * @param executionId 执行记录唯一标识
     * @param eventType   事件类型（如 "tool_call"、"llm_invoke"、"step_complete"）
     * @param stepName    步骤名称
     * @param payloadJson 事件负载（JSON 格式）
     * @return 完成信号
     */
    Mono<Void> appendEvent(UUID executionId, String eventType, String stepName, String payloadJson);

    /**
     * 标记一次 Agent 执行已完成。
     *
     * @param executionId 执行记录唯一标识
     * @param status      最终状态（如 "success"、"error"、"cancelled"）
     * @param tokensUsed  累计 Token 消耗（可空）
     * @param cost        累计成本（可空）
     * @param outputJson  输出结果（JSON 格式，可空）
     * @param error       错误信息（可空，仅失败时填写）
     * @return 完成信号
     */
    Mono<Void> finishExecution(UUID executionId, String status, Integer tokensUsed,
                               Double cost, String outputJson, String error);

    /**
     * Agent 执行记录。
     */
    record AgentExecution(
            /** 执行记录唯一标识 */
            UUID id,
            /** Agent 名称 */
            String agentName,
            /** 输入参数（JSON 格式） */
            String inputJson,
            /** 执行状态（"running"、"success"、"error"、"cancelled"） */
            String status,
            /** 开始时间 */
            Instant startedAt,
            /** 结束时间（可空，未完成时为 null） */
            Instant finishedAt,
            /** 累计 Token 消耗（可空） */
            Integer tokensUsed,
            /** 累计成本（可空） */
            Double cost,
            /** 输出结果（JSON 格式，可空） */
            String outputJson,
            /** 错误信息（可空） */
            String error
    ) {}
}
