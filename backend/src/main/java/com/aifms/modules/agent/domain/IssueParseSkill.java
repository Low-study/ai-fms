package com.aifms.modules.agent.domain;

import reactor.core.publisher.Mono;

/**
 * 工单解析技能接口。
 * 从文档解析结果中提取结构化工单信息（标题、描述、原始文本）。
 *
 * @author aifms
 */
public interface IssueParseSkill {

    /**
     * 从已解析的文档中提取工单结构化信息。
     *
     * @param document 已解析的文档（来自 {@link DocumentParserPort#parse}）
     * @return 结构化工单信息
     */
    Mono<ParsedIssue> parse(DocumentParserPort.ParsedDocument document);
}
