package com.aifms.modules.agent.infrastructure;

import com.aifms.modules.agent.domain.PromptTemplatePort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * {@link PromptTemplatePort} 的数据库适配器实现。
 * 从 prompt_templates 表读取版本化提示词模板。
 *
 * @author aifms
 */
@Component
public class PromptTemplatePortAdapter implements PromptTemplatePort {

    private final PromptTemplateRepository repository;

    public PromptTemplatePortAdapter(PromptTemplateRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<PromptTemplate> findByNameVersion(String name, int version) {
        return repository.findByNameAndVersion(name, version)
                .map(entity -> new PromptTemplate(
                        entity.getName(),
                        entity.getVersion(),
                        entity.getSystemTemplate(),
                        entity.getUserTemplate()
                ));
    }
}
