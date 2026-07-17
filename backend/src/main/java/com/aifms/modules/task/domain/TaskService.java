package com.aifms.modules.task.domain;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 异步任务队列抽象接口。
 * 定义任务提交、消费、进度推送、状态查询的标准契约。
 * 上层模块（如 Agent Runtime）仅依赖本接口，不感知具体实现。
 *
 * @param <T> 任务负载数据类型
 */
public interface TaskService<T> {

    /**
     * 提交任务到队列。
     *
     * @param payload 任务负载数据
     * @return 任务凭据，包含 ticketId 用于后续状态查询
     */
    Mono<TaskTicket> submit(T payload);

    /**
     * 以消费者组方式消费任务。
     * 每个消费者组内消息只会被一个消费者处理。
     *
     * @param consumerGroup 消费者组名称
     * @return 任务进度流，每条记录包含回执信息用于 ack
     */
    Flux<TaskProgress<T>> consume(String consumerGroup);

    /**
     * 查询任务当前状态。
     *
     * @param ticketId 任务凭据 ID
     * @return 当前任务状态，若 ticketId 不存在返回空
     */
    Mono<TaskStatus> status(String ticketId);

    /**
     * 确认任务已处理完成。
     *
     * @param receipt 消费回执（格式: group:recordId）
     * @return 完成信号
     */
    Mono<Void> ack(String receipt);

    /**
     * 发布任务进度更新。
     * 通过 Pub/Sub 广播，所有订阅者均可收到。
     *
     * @param ticketId 任务凭据 ID
     * @param progress 进度快照
     * @return 完成信号
     */
    Mono<Void> publishProgress(String ticketId, TaskProgress<T> progress);

    /**
     * 订阅任务进度更新流。
     * 通过 Pub/Sub 监听，实时接收进度推送。
     *
     * @param ticketId 任务凭据 ID
     * @return 进度快照流
     */
    Flux<TaskProgress<T>> streamProgress(String ticketId);
}
