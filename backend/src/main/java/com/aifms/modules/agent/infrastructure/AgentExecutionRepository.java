package com.aifms.modules.agent.infrastructure;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * {@link AgentExecutionEntity} 的 R2DBC 数据访问接口。
 *
 * @author aifms
 */
@Repository
public interface AgentExecutionRepository extends ReactiveCrudRepository<AgentExecutionEntity, UUID> {
}
