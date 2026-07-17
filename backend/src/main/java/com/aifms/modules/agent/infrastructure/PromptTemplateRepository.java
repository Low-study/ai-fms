package com.aifms.modules.agent.infrastructure;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * {@link PromptTemplateEntity} 的 R2DBC 数据访问接口。
 *
 * @author aifms
 */
@Repository
public interface PromptTemplateRepository extends ReactiveCrudRepository<PromptTemplateEntity, UUID> {

    /**
     * 按模板名称和版本号精确查找。
     *
     * @param name    模板名称
     * @param version 模板版本号
     * @return 匹配的模板实体（可能为空）
     */
    Mono<PromptTemplateEntity> findByNameAndVersion(String name, int version);
}
