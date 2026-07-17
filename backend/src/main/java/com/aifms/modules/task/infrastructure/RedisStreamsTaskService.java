package com.aifms.modules.task.infrastructure;

import com.aifms.modules.task.domain.TaskProgress;
import com.aifms.modules.task.domain.TaskService;
import com.aifms.modules.task.domain.TaskStatus;
import com.aifms.modules.task.domain.TaskTicket;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 基于 Redis Streams + Pub/Sub 的 TaskService 实现。
 * <ul>
 *   <li>任务队列: Redis Streams (XADD / XREADGROUP / XACK)</li>
 *   <li>状态存储: Redis Hash (task:ticket:{ticketId})</li>
 *   <li>进度广播: Redis Pub/Sub (task-progress:{ticketId})</li>
 * </ul>
 * <p>
 * 全链路响应式，不使用 {@code .block()}。
 * 依赖 ReactiveRedisTemplate（已在 {@code RedisConfig} 中声明为 Bean）。
 * </p>
 */
@Service
public class RedisStreamsTaskService implements TaskService<Object> {

    private static final String STREAM_KEY = "task-stream";
    private static final String TICKET_KEY_PREFIX = "task:ticket:";
    private static final String PROGRESS_CHANNEL_PREFIX = "task-progress:";

    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    public RedisStreamsTaskService(ReactiveRedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ── submit ──

    /**
     * {@inheritDoc}
     * <p>
     * 实现步骤：
     * <ol>
     *   <li>生成 ticketId，创建 TaskTicket 并写入 Redis Hash</li>
     *   <li>将负载包装进 MapRecord 并 XADD 到 task-stream</li>
     *   <li>返回 TaskTicket 供调用方追踪</li>
     * </ol>
     */
    @Override
    public Mono<TaskTicket> submit(Object payload) {
        String ticketId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        TaskTicket ticket = new TaskTicket(
                ticketId, payload.getClass().getName(), TaskStatus.QUEUED, now);

        Map<String, Object> ticketFields = Map.of(
                "ticketId", ticketId,
                "payloadType", ticket.getPayloadType(),
                "status", TaskStatus.QUEUED.name(),
                "createdAt", now.toString());

        Map<String, Object> recordBody = new LinkedHashMap<>();
        recordBody.put("_ticketId", ticketId);
        recordBody.put("_payloadType", ticket.getPayloadType());
        recordBody.put("_payload", payload);

        @SuppressWarnings({"unchecked", "rawtypes"})
        MapRecord<String, Object, Object> record = (MapRecord) StreamRecords
                .mapBacked(recordBody)
                .withStreamKey(STREAM_KEY);

        return redisTemplate.opsForHash()
                .putAll(TICKET_KEY_PREFIX + ticketId, ticketFields)
                .then(redisTemplate.opsForStream().add(record))
                .thenReturn(ticket);
    }

    // ── consume ──

    /**
     * {@inheritDoc}
     * <p>
     * 创建消费者组（若已存在则忽略），然后以 {@link ReadOffset#lastConsumed()} 模式
     * 消费新消息。每条消息包装为 TaskProgress 返回，receipt 格式为
     * {@code group:recordId:ticketId}。
     * </p>
     * <p>
     * 使用 {@code .repeat()} 实现长轮询：当流中无新消息时自动重新订阅，
     * 不会阻塞线程。
     * </p>
     */
    @Override
    @SuppressWarnings("unchecked")
    public Flux<TaskProgress<Object>> consume(String consumerGroup) {
        String consumerName = consumerGroup + "-" + UUID.randomUUID().toString().substring(0, 8);
        Consumer consumer = Consumer.from(consumerGroup, consumerName);
        StreamOffset<String> offset = StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed());

        return ensureConsumerGroup(consumerGroup)
                .thenMany(
                        redisTemplate.opsForStream()
                                .read(consumer, offset)
                                .flatMap(record -> {
                                    Map<Object, Object> body = record.getValue();
                                    String ticketId = String.valueOf(body.get("_ticketId"));
                                    Object payload = body.get("_payload");
                                    String receipt = consumerGroup + ":"
                                            + record.getId().getValue() + ":" + ticketId;

                                    TaskProgress<Object> progress = new TaskProgress<>();
                                    progress.setTicketId(ticketId);
                                    progress.setData(payload);
                                    progress.setReceipt(receipt);
                                    progress.setStepName("CONSUMED");
                                    progress.setPercentage(0);
                                    progress.setMessage("Task dequeued from stream");

                                    return updateTicketStatus(ticketId, TaskStatus.RUNNING)
                                            .thenReturn(progress);
                                })
                )
                .repeatWhen(repeat -> repeat.delayElements(Duration.ofSeconds(2)))
                .onBackpressureBuffer();
    }

    // ── status ──

    /**
     * {@inheritDoc}
     * <p>
     * 从 Redis Hash 中读取 ticket 的 status 字段。
     * </p>
     */
    @Override
    public Mono<TaskStatus> status(String ticketId) {
        return redisTemplate.<String, String>opsForHash()
                .get(TICKET_KEY_PREFIX + ticketId, "status")
                .map(status -> TaskStatus.valueOf((String) status));
    }

    // ── ack ──

    /**
     * {@inheritDoc}
     * <p>
     * 解析 receipt 中的 group / recordId / ticketId，
     * 执行 XACK 确认消费，并将 ticket 状态更新为 ACKED。
     * </p>
     */
    @Override
    public Mono<Void> ack(String receipt) {
        String[] parts = receipt.split(":", 3);
        if (parts.length < 3) {
            return Mono.error(new IllegalArgumentException(
                    "无效的回执格式，期望 group:recordId:ticketId，实际: " + receipt));
        }
        String group = parts[0];
        String recordId = parts[1];
        String ticketId = parts[2];

        return redisTemplate.opsForStream()
                .acknowledge(STREAM_KEY, group, recordId)
                .then(updateTicketStatus(ticketId, TaskStatus.ACKED));
    }

    // ── publishProgress ──

    /**
     * {@inheritDoc}
     * <p>
     * 通过 Redis Pub/Sub 将进度快照发布到 {@code task-progress:{ticketId}} 频道。
     * </p>
     */
    @Override
    public Mono<Void> publishProgress(String ticketId, TaskProgress<Object> progress) {
        progress.setTicketId(ticketId);
        return redisTemplate.convertAndSend(PROGRESS_CHANNEL_PREFIX + ticketId, progress).then();
    }

    // ── streamProgress ──

    /**
     * {@inheritDoc}
     * <p>
     * 订阅 Redis Pub/Sub 频道 {@code task-progress:{ticketId}}，
     * 实时接收进度推送。返回的 Flux 不会自动终止，调用方需自行取消订阅。
     * </p>
     */
    @Override
    @SuppressWarnings("unchecked")
    public Flux<TaskProgress<Object>> streamProgress(String ticketId) {
        return redisTemplate.listenToChannel(PROGRESS_CHANNEL_PREFIX + ticketId)
                .map(message -> {
                    Object body = message.getMessage();
                    if (body instanceof TaskProgress) {
                        return (TaskProgress<Object>) body;
                    }
                    // 兜底: 如果是 LinkedHashMap 则手动映射
                    if (body instanceof Map) {
                        Map<String, Object> map = (Map<String, Object>) body;
                        TaskProgress<Object> progress = new TaskProgress<>();
                        progress.setTicketId((String) map.get("ticketId"));
                        progress.setStepName((String) map.get("stepName"));
                        progress.setPercentage(map.get("percentage") != null
                                ? ((Number) map.get("percentage")).intValue() : 0);
                        progress.setMessage((String) map.get("message"));
                        progress.setData(map.get("data"));
                        progress.setReceipt((String) map.get("receipt"));
                        return progress;
                    }
                    return null;
                })
                .filter(progress -> progress != null);
    }

    // ── 私有辅助方法 ──

    /**
     * 确保消费者组存在，若已存在或 stream 未创建则静默忽略。
     */
    private Mono<Void> ensureConsumerGroup(String consumerGroup) {
        return redisTemplate.opsForStream()
                .createGroup(STREAM_KEY, consumerGroup)
                .onErrorResume(e -> Mono.empty())
                .then();
    }

    /**
     * 更新 Redis Hash 中 ticket 的状态字段。
     */
    private Mono<Void> updateTicketStatus(String ticketId, TaskStatus status) {
        return redisTemplate.opsForHash()
                .put(TICKET_KEY_PREFIX + ticketId, "status", status.name())
                .then();
    }
}
