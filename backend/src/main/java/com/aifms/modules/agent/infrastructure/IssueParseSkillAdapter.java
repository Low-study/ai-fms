package com.aifms.modules.agent.infrastructure;

import com.aifms.modules.agent.domain.ChatModelPort;
import com.aifms.modules.agent.domain.DocumentParserPort;
import com.aifms.modules.agent.domain.IssueParseSkill;
import com.aifms.modules.agent.domain.ParsedIssue;
import com.aifms.modules.agent.domain.PromptTemplatePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 工单解析技能适配器。
 * 通过 ChatModelPort 调用 LLM，从文档解析结果中提取工单结构化信息。
 *
 * @author aifms
 */
@Service
public class IssueParseSkillAdapter implements IssueParseSkill {

    private static final String TEMPLATE_NAME = "issue_parse_skill";
    private static final int TEMPLATE_VERSION = 1;
    private static final String DEFAULT_MODEL = "gpt-4o";
    private static final double DEFAULT_TEMPERATURE = 0.3;

    private final ChatModelPort chatModelPort;
    private final PromptTemplatePort promptTemplatePort;
    private final ObjectMapper objectMapper;

    public IssueParseSkillAdapter(ChatModelPort chatModelPort,
                                  PromptTemplatePort promptTemplatePort,
                                  ObjectMapper objectMapper) {
        this.chatModelPort = chatModelPort;
        this.promptTemplatePort = promptTemplatePort;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<ParsedIssue> parse(DocumentParserPort.ParsedDocument document) {
        return promptTemplatePort.findByNameVersion(TEMPLATE_NAME, TEMPLATE_VERSION)
                .flatMap(template -> {
                    String systemPrompt = template.systemTemplate();
                    String userPrompt = template.userTemplate() + "\n\n" + document.rawText();
                    ChatModelPort.ChatRequest request = new ChatModelPort.ChatRequest(
                            systemPrompt, userPrompt, DEFAULT_MODEL, DEFAULT_TEMPERATURE);
                    return chatModelPort.call(request);
                })
                .flatMap(response -> Mono.fromCallable(() ->
                        objectMapper.readValue(extractJson(response.content()), ParsedIssue.class)
                ).subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 从 LLM 回复中提取 JSON 内容。
     * 处理 markdown 代码块包裹（```json ... ```）的情况。
     */
    private String extractJson(String content) {
        if (content == null || content.isBlank()) {
            return "{}";
        }
        String trimmed = content.trim();
        // 移除 markdown 代码块标记
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
}
