package com.aifms.modules.agent.domain;

import reactor.core.publisher.Mono;

/**
 * 工单分类技能接口。
 * 对已解析的工单进行智能分类，补充类别、优先级、严重程度等标注信息。
 *
 * @author aifms
 */
public interface IssueClassifySkill {

    /**
     * 对工单进行分类标注。
     *
     * @param issue 已解析的工单信息
     * @return 分类标注后的工单
     */
    Mono<ClassifiedIssue> classify(ParsedIssue issue);
}
