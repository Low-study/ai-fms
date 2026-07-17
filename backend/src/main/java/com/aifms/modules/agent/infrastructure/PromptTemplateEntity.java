package com.aifms.modules.agent.infrastructure;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * "prompt_templates" 表的 R2DBC 持久化实体。
 * 与 {@link com.aifms.modules.agent.domain.PromptTemplatePort.PromptTemplate} 对应。
 * 仅包含 getter/setter，不含任何领域逻辑。
 *
 * @author aifms
 */
@Table("prompt_templates")
public class PromptTemplateEntity {

    /** 主键 */
    @Id
    private UUID id;

    /** 模板名称 */
    private String name;

    /** 模板版本号 */
    private int version;

    /** 系统提示词模板 */
    private String systemTemplate;

    /** 用户提示词模板 */
    private String userTemplate;

    /** 创建时间 */
    private Instant createdAt;

    // ── Getters & Setters ──

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public String getSystemTemplate() { return systemTemplate; }
    public void setSystemTemplate(String systemTemplate) { this.systemTemplate = systemTemplate; }

    public String getUserTemplate() { return userTemplate; }
    public void setUserTemplate(String userTemplate) { this.userTemplate = userTemplate; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
