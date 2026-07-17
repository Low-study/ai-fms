package com.aifms.modules.agent.infrastructure;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * {@link AgentExecutionEventEntity} 的 R2DBC 数据访问接口。
 *
 * @author aifms
 */
@Repository
public interface AgentExecutionEventRepository extends ReactiveCrudRepository<AgentExecutionEventEntity, UUID> {

    /**
     * 按执行记录 ID 查询所有步骤事件（按创建时间升序排列）。
     *
     * @param executionId 执行记录 ID
     * @return 该执行记录的所有事件
     */
    Flux<AgentExecutionEventEntity> findByExecutionId(UUID executionId);
}
