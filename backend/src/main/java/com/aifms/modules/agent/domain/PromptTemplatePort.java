package com.aifms.modules.agent.domain;

import reactor.core.publisher.Mono;

/**
 * 提示词模板管理抽象端口。
 * 按名称和版本号查找预定义的提示词模板。
 * 基础设施层负责对接模板存储（如数据库、文件系统、配置中心）。
 *
 * @author aifms
 */
public interface PromptTemplatePort {

    /**
     * 按名称和版本号查找提示词模板。
     *
     * @param name    模板名称
     * @param version 模板版本号（正整数，通常为 1）
     * @return 匹配的提示词模板
     */
    Mono<PromptTemplate> findByNameVersion(String name, int version);

    /**
     * 提示词模板结构。
     * 包含系统提示词和用户提示词两部分，运行时替换占位符变量。
     */
    record PromptTemplate(
            /** 模板名称 */
            String name,
            /** 模板版本号 */
            int version,
            /** 系统提示词模板（可包含占位符如 {{role}}） */
            String systemTemplate,
            /** 用户提示词模板（可包含占位符如 {{input}}） */
            String userTemplate
    ) {}
}
