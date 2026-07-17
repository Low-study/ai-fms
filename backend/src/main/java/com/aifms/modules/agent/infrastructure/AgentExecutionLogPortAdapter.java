package com.aifms.modules.agent.infrastructure;

import com.aifms.modules.agent.domain.AgentExecutionLogPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * {@link AgentExecutionLogPort} 的数据库适配器实现。
 * 将 Agent 执行日志持久化到 agent_executions 和 agent_execution_events 表。
 * 所有写操作均采用 fail-safe 策略：持久化失败时记录日志但不向上传播异常。
 *
 * @author aifms
 */
@Component
public class AgentExecutionLogPortAdapter implements AgentExecutionLogPort {

    private static final Logger log = LoggerFactory.getLogger(AgentExecutionLogPortAdapter.class);

    private final AgentExecutionRepository executionRepository;
    private final AgentExecutionEventRepository eventRepository;

    public AgentExecutionLogPortAdapter(AgentExecutionRepository executionRepository,
                                        AgentExecutionEventRepository eventRepository) {
        this.executionRepository = executionRepository;
        this.eventRepository = eventRepository;
    }

    @Override
    public Mono<AgentExecution> startExecution(String agentName, String inputJson) {
        AgentExecutionEntity entity = new AgentExecutionEntity();
        entity.setAgentName(agentName);
        entity.setInputJson(inputJson);
        entity.setStatus("QUEUED");
        entity.setStartedAt(Instant.now());
        entity.setCreatedAt(Instant.now());

        return executionRepository.save(entity)
                .onErrorResume(e -> {
                    log.error("Failed to persist agent execution start: agentName={}, error={}",
                            agentName, e.getMessage(), e);
                    AgentExecutionEntity fallback = new AgentExecutionEntity();
                    fallback.setId(UUID.randomUUID());
                    fallback.setAgentName(agentName);
                    fallback.setInputJson(inputJson);
                    fallback.setStatus("QUEUED");
                    fallback.setStartedAt(Instant.now());
                    fallback.setCreatedAt(Instant.now());
                    return Mono.just(fallback);
                })
                .map(saved -> new AgentExecution(
                        saved.getId(),
                        saved.getAgentName(),
                        saved.getInputJson(),
                        saved.getStatus(),
                        saved.getStartedAt(),
                        saved.getFinishedAt(),
                        saved.getTokensUsed(),
                        saved.getCost() != null ? saved.getCost().doubleValue() : null,
                        saved.getOutputJson(),
                        saved.getError()
                ));
    }

    @Override
    public Mono<Void> appendEvent(UUID executionId, String eventType, String stepName, String payloadJson) {
        AgentExecutionEventEntity entity = new AgentExecutionEventEntity();
        entity.setExecutionId(executionId);
        entity.setEventType(eventType);
        entity.setStepName(stepName);
        entity.setPayloadJson(payloadJson);
        entity.setCreatedAt(Instant.now());

        return eventRepository.save(entity)
                .onErrorResume(e -> {
                    log.error("Failed to persist execution event: executionId={}, eventType={}, stepName={}, error={}",
                            executionId, eventType, stepName, e.getMessage(), e);
                    return Mono.empty();
                })
                .then();
    }

    @Override
    public Mono<Void> finishExecution(UUID executionId, String status, Integer tokensUsed,
                                      Double cost, String outputJson, String error) {
        return executionRepository.findById(executionId)
                .flatMap(entity -> {
                    entity.setStatus(status);
                    entity.setFinishedAt(Instant.now());
                    entity.setTokensUsed(tokensUsed);
                    entity.setCost(cost != null ? BigDecimal.valueOf(cost) : null);
                    entity.setOutputJson(outputJson);
                    entity.setError(error);
                    return executionRepository.save(entity);
                })
                .onErrorResume(e -> {
                    log.error("Failed to update execution finish: executionId={}, status={}, error={}",
                            executionId, status, e.getMessage(), e);
                    return Mono.empty();
                })
                .then();
    }
}
