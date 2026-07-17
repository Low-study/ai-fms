package com.aifms.modules.agent.domain;

import java.util.List;

/**
 * 分类后的工单信息。
 * 在原始解析工单的基础上补充分类、优先级、严重程度等智能标注信息。
 */
public record ClassifiedIssue(
        /** 原始解析工单 */
        ParsedIssue issue,
        /** 工单分类（如 "bug"、"feature_request"、"support"） */
        String category,
        /** 优先级（如 "P0"、"P1"、"P2"、"P3"） */
        String priority,
        /** 严重程度（如 "critical"、"major"、"minor"、"trivial"） */
        String severity,
        /** 受影响的系统/模块 */
        String system,
        /** 指派的处理人 */
        String assignee,
        /** 标签列表 */
        List<String> tags
) {}
