package com.aifms.modules.agent.domain;

import reactor.core.publisher.Mono;

/**
 * 智能问答技能接口。
 * 基于分类工单生成智能回复内容。
 *
 * @author aifms
 */
public interface QaSkill {

    /**
     * 为工单生成智能回复。
     *
     * @param issue 分类后的工单信息
     * @return 智能回复文本
     */
    Mono<String> generateReply(ClassifiedIssue issue);
}
