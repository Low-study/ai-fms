package com.aifms.modules.task.application;

import com.aifms.modules.task.domain.TaskProgress;
import com.aifms.modules.task.domain.TaskService;
import com.aifms.modules.task.domain.TaskStatus;
import com.aifms.modules.task.domain.TaskTicket;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * 异步任务应用服务 — 编排任务生命周期。
 * <p>
 * 内部委托给 {@link TaskService} 接口，不直接依赖 Redis 等基础设施。
 * 其他模块（如 Agent Runtime）可通过注入本服务来提交和追踪任务。
 * </p>
 */
@Service
public class TaskApplicationService {

    private final TaskService<Object> taskService;

    public TaskApplicationService(TaskService<Object> taskService) {
        this.taskService = taskService;
    }

    /**
     * 提交任务到队列。
     *
     * @param payload 任务负载数据
     * @return 任务凭据
     */
    public Mono<TaskTicket> submitTask(Object payload) {
        return taskService.submit(payload);
    }

    /**
     * 提交任务并等待完成。
     * 提交后订阅进度流，通过定时轮询状态检测终态（DONE / FAILED），
     * 达到终态后终止流。全链路响应式，不使用 {@code .block()}。
     *
     * @param payload 任务负载数据
     * @return 最终进度快照
     */
    public Mono<TaskProgress<Object>> submitAndWait(Object payload) {
        return taskService.submit(payload)
                .flatMap(ticket -> {
                    Flux<TaskStatus> terminal = Flux.interval(Duration.ofSeconds(1))
                            .flatMap(i -> taskService.status(ticket.getTicketId()))
                            .filter(s -> s == TaskStatus.DONE || s == TaskStatus.FAILED)
                            .take(1);

                    return taskService.streamProgress(ticket.getTicketId())
                            .takeUntilOther(terminal)
                            .last();
                });
    }

    /**
     * 以消费者组方式消费任务流。
     *
     * @param consumerGroup 消费者组名称
     * @return 任务进度流
     */
    public Flux<TaskProgress<Object>> consumeTasks(String consumerGroup) {
        return taskService.consume(consumerGroup);
    }

    /**
     * 发布任务进度更新。
     *
     * @param ticketId 任务凭据 ID
     * @param progress 进度快照
     * @return 完成信号
     */
    public Mono<Void> publishProgress(String ticketId, TaskProgress<Object> progress) {
        return taskService.publishProgress(ticketId, progress);
    }

    /**
     * 确认任务处理完成。
     *
     * @param receipt 消费回执
     * @return 完成信号
     */
    public Mono<Void> acknowledgeTask(String receipt) {
        return taskService.ack(receipt);
    }

    /**
     * 查询任务状态。
     *
     * @param ticketId 任务凭据 ID
     * @return 当前状态
     */
    public Mono<TaskStatus> getTaskStatus(String ticketId) {
        return taskService.status(ticketId);
    }

    /**
     * 订阅任务进度流。
     *
     * @param ticketId 任务凭据 ID
     * @return 进度流
     */
    public Flux<TaskProgress<Object>> observeProgress(String ticketId) {
        return taskService.streamProgress(ticketId);
    }
}
