package com.aifms.modules.agent.domain;

import java.util.UUID;

/**
 * Agent 异步任务负载数据。
 * 作为 {@link com.aifms.modules.task.domain.TaskService#submit} 的输入参数，
 * 封装了待处理的指摘 ID。
 */
public record AgentTask(
        /** 指摘唯一标识 */
        UUID findingId
) {}
