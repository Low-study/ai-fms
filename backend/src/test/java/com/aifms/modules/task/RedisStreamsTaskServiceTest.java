package com.aifms.modules.task;

import com.aifms.modules.task.domain.TaskProgress;
import com.aifms.modules.task.domain.TaskService;
import com.aifms.modules.task.domain.TaskStatus;
import com.aifms.modules.task.domain.TaskTicket;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RedisStreamsTaskService 集成测试。
 * 使用 Testcontainers 启动 redis:7-alpine 容器，
 * 通过 ReactiveRedisTemplate 验证完整的任务生命周期。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration"
        })
@Testcontainers
class RedisStreamsTaskServiceTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private TaskService<Object> taskService;

    // ── submit + status ──

    /**
     * 提交任务后应立即返回 QUEUED 状态的凭据。
     */
    @Test
    void shouldReturnQueuedTicket_whenSubmitTask() {
        String payload = "hello-task";

        TaskTicket ticket = taskService.submit(payload).block(Duration.ofSeconds(10));

        assertNotNull(ticket);
        assertNotNull(ticket.getTicketId());
        assertEquals(TaskStatus.QUEUED, ticket.getStatus());
        assertEquals(String.class.getName(), ticket.getPayloadType());
        assertNotNull(ticket.getCreatedAt());
    }

    /**
     * 提交后可通过 ticketId 查询到 QUEUED 状态。
     */
    @Test
    void shouldReturnQueuedStatus_whenQueryAfterSubmit() {
        String payload = "query-test";

        TaskTicket ticket = taskService.submit(payload).block(Duration.ofSeconds(10));

        TaskStatus status = taskService.status(ticket.getTicketId()).block(Duration.ofSeconds(10));

        assertEquals(TaskStatus.QUEUED, status);
    }

    // ── consume + ack ──

    /**
     * 完整生命周期：submit → consume → ack → 状态验证。
     */
    @Test
    void shouldConsumeAndAckTask_whenSubmitThenConsume() {
        String payload = "lifecycle-test";
        String consumerGroup = "test-group-" + UUID.randomUUID().toString().substring(0, 8);

        // 1. Submit
        TaskTicket ticket = taskService.submit(payload).block(Duration.ofSeconds(10));
        assertEquals(TaskStatus.QUEUED, ticket.getStatus());

        // 2. Consume — 应该读取到刚提交的消息
        TaskProgress<Object> consumed = taskService.consume(consumerGroup)
                .next()
                .block(Duration.ofSeconds(15));

        assertNotNull(consumed);
        assertEquals(ticket.getTicketId(), consumed.getTicketId());
        assertNotNull(consumed.getReceipt());
        assertTrue(consumed.getReceipt().contains(":"),
                "receipt 格式应为 group:recordId:ticketId");
        assertEquals("lifecycle-test", consumed.getData());

        // 3. 消费后状态应为 RUNNING
        TaskStatus runningStatus = taskService.status(ticket.getTicketId())
                .block(Duration.ofSeconds(10));
        assertEquals(TaskStatus.RUNNING, runningStatus);

        // 4. Ack
        taskService.ack(consumed.getReceipt()).block(Duration.ofSeconds(10));

        // 5. Ack 后状态应为 ACKED
        TaskStatus ackedStatus = taskService.status(ticket.getTicketId())
                .block(Duration.ofSeconds(10));
        assertEquals(TaskStatus.ACKED, ackedStatus);
    }

    // ── publishProgress + streamProgress ──

    /**
     * 发布进度后，订阅者应能收到相同的进度快照。
     */
    @Test
    void shouldReceiveProgress_whenPublishAndStream() {
        String ticketId = "progress-test-" + UUID.randomUUID().toString().substring(0, 8);
        TaskProgress<Object> sent = new TaskProgress<>();
        sent.setStepName("PARSE_FILE");
        sent.setPercentage(42);
        sent.setMessage("正在解析文件内容...");
        sent.setData("intermediate-result");

        // 先订阅、再发布（通过 thenAwait 保证订阅已激活）
        Flux<TaskProgress<Object>> stream = taskService.streamProgress(ticketId)
                .take(1)
                .timeout(Duration.ofSeconds(10));

        // 延迟后发布进度
        Flux.interval(Duration.ofMillis(300))
                .take(1)
                .flatMap(tick -> taskService.publishProgress(ticketId, sent))
                .subscribe();

        TaskProgress<Object> received = stream.blockFirst(Duration.ofSeconds(10));

        assertNotNull(received, "应收到进度快照");
        assertEquals("PARSE_FILE", received.getStepName());
        assertEquals(42, received.getPercentage());
        assertEquals("正在解析文件内容...", received.getMessage());
    }

    /**
     * 提交任务后生成的 ticketId 可用作进度频道的标识。
     */
    @Test
    void shouldUseTicketIdAsProgressChannel() {
        String payload = "channel-test";
        TaskTicket ticket = taskService.submit(payload).block(Duration.ofSeconds(10));
        String ticketId = ticket.getTicketId();

        TaskProgress<Object> sent = new TaskProgress<>();
        sent.setStepName("INIT");
        sent.setPercentage(10);
        sent.setMessage("初始化完成");

        // 订阅 + 延迟发布
        Flux<TaskProgress<Object>> stream = taskService.streamProgress(ticketId)
                .take(1)
                .timeout(Duration.ofSeconds(10));

        Flux.interval(Duration.ofMillis(300))
                .take(1)
                .flatMap(tick -> taskService.publishProgress(ticketId, sent))
                .subscribe();

        TaskProgress<Object> received = stream.blockFirst(Duration.ofSeconds(10));

        assertNotNull(received);
        assertEquals("INIT", received.getStepName());
        assertEquals(10, received.getPercentage());
    }

    // ── 边界场景 ──

    /**
     * 无效 receipt 格式应抛出异常。
     */
    @Test
    void shouldThrowException_whenInvalidReceiptFormat() {
        assertThrows(Exception.class, () ->
                taskService.ack("invalid-receipt").block(Duration.ofSeconds(10)));
    }

    /**
     * 同一消费者组内每条消息只被消费一次。
     */
    @Test
    void shouldConsumeEachMessageOnce_whenSameConsumerGroup() {
        String group = "dedup-group-" + UUID.randomUUID().toString().substring(0, 8);

        // 提交两条消息
        TaskTicket t1 = taskService.submit("msg-1").block(Duration.ofSeconds(10));
        TaskTicket t2 = taskService.submit("msg-2").block(Duration.ofSeconds(10));

        // 同一消费者组分别消费
        Flux<TaskProgress<Object>> stream = taskService.consume(group).take(2).timeout(Duration.ofSeconds(20));

        java.util.List<TaskProgress<Object>> results = stream.collectList().block(Duration.ofSeconds(20));

        assertNotNull(results);
        assertEquals(2, results.size());
        // 确认两条消息都被消费
        assertTrue(results.stream().anyMatch(p -> t1.getTicketId().equals(p.getTicketId())));
        assertTrue(results.stream().anyMatch(p -> t2.getTicketId().equals(p.getTicketId())));
    }
}
