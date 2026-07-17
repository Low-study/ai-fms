package com.aifms.modules.agent.infrastructure;

import com.aifms.modules.agent.domain.ChatModelPort;
import com.aifms.modules.agent.domain.ClassifiedIssue;
import com.aifms.modules.agent.domain.PromptTemplatePort;
import com.aifms.modules.agent.domain.ReportDraftSkill;
import com.aifms.modules.agent.domain.SimilarIssues;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 报告起草技能适配器。
 * 通过 ChatModelPort 调用 LLM，基于分类工单和相似工单生成处理报告草稿。
 *
 * @author aifms
 */
@Service
public class ReportDraftSkillAdapter implements ReportDraftSkill {

    private static final String TEMPLATE_NAME = "report_draft_skill";
    private static final int TEMPLATE_VERSION = 1;
    private static final String DEFAULT_MODEL = "gpt-4o";
    private static final double DEFAULT_TEMPERATURE = 0.5;

    private final ChatModelPort chatModelPort;
    private final PromptTemplatePort promptTemplatePort;
    private final ObjectMapper objectMapper;

    public ReportDraftSkillAdapter(ChatModelPort chatModelPort,
                                   PromptTemplatePort promptTemplatePort,
                                   ObjectMapper objectMapper) {
        this.chatModelPort = chatModelPort;
        this.promptTemplatePort = promptTemplatePort;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<String> draftReport(ClassifiedIssue issue, SimilarIssues similar) {
        return promptTemplatePort.findByNameVersion(TEMPLATE_NAME, TEMPLATE_VERSION)
                .flatMap(template -> {
                    String systemPrompt = template.systemTemplate();
                    String userPrompt = template.userTemplate()
                            + "\n\nIssue:\n" + toJson(issue)
                            + "\n\nSimilar issues:\n" + toJson(similar);
                    ChatModelPort.ChatRequest request = new ChatModelPort.ChatRequest(
                            systemPrompt, userPrompt, DEFAULT_MODEL, DEFAULT_TEMPERATURE);
                    return chatModelPort.call(request);
                })
                .flatMap(response -> Mono.fromCallable(() -> {
                    String content = response.content();
                    // 尝试从 JSON 响应中提取 report 字段
                    try {
                        ReportJson json = objectMapper.readValue(extractJson(content), ReportJson.class);
                        return json.report();
                    } catch (JsonProcessingException e) {
                        // 如果解析失败，直接返回原始内容
                        return extractJson(content);
                    }
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 从 LLM 回复中提取 JSON 内容。
     */
    private String extractJson(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('\n');
            if (start > 0) {
                int end = trimmed.lastIndexOf("```");
                if (end > start) {
                    trimmed = trimmed.substring(start + 1, end).trim();
                }
            }
        }
        return trimmed;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return obj.toString();
        }
    }

    /**
     * LLM 返回的报告 JSON 中间 DTO。
     */
    record ReportJson(String report) {}
}
