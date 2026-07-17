package com.aifms.modules.agent.infrastructure;

import com.aifms.modules.agent.domain.ChatModelPort;
import com.aifms.modules.agent.domain.ClassifiedIssue;
import com.aifms.modules.agent.domain.IssueClassifySkill;
import com.aifms.modules.agent.domain.ParsedIssue;
import com.aifms.modules.agent.domain.PromptTemplatePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 工单分类技能适配器。
 * 通过 ChatModelPort 调用 LLM，对已解析的工单进行智能分类标注。
 *
 * @author aifms
 */
@Service
public class IssueClassifySkillAdapter implements IssueClassifySkill {

    private static final String TEMPLATE_NAME = "issue_classify_skill";
    private static final int TEMPLATE_VERSION = 1;
    private static final String DEFAULT_MODEL = "deepseek-chat";
    private static final double DEFAULT_TEMPERATURE = 0.3;

    private final ChatModelPort chatModelPort;
    private final PromptTemplatePort promptTemplatePort;
    private final ObjectMapper objectMapper;

    public IssueClassifySkillAdapter(ChatModelPort chatModelPort,
                                     PromptTemplatePort promptTemplatePort,
                                     ObjectMapper objectMapper) {
        this.chatModelPort = chatModelPort;
        this.promptTemplatePort = promptTemplatePort;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<ClassifiedIssue> classify(ParsedIssue issue) {
        return promptTemplatePort.findByNameVersion(TEMPLATE_NAME, TEMPLATE_VERSION)
                .flatMap(template -> {
                    String systemPrompt = template.systemTemplate();
                    String userPrompt = template.userTemplate() + "\n\n"
                            + "Title: " + issue.title() + "\n"
                            + "Description: " + issue.description();
                    ChatModelPort.ChatRequest request = new ChatModelPort.ChatRequest(
                            systemPrompt, userPrompt, DEFAULT_MODEL, DEFAULT_TEMPERATURE);
                    return chatModelPort.call(request);
                })
                .flatMap(response -> Mono.fromCallable(() -> {
                    ClassifiedIssueJson json = objectMapper.readValue(
                            extractJson(response.content()), ClassifiedIssueJson.class);
                    return new ClassifiedIssue(
                            issue,
                            json.category(),
                            json.priority(),
                            json.severity(),
                            json.system(),
                            json.assignee(),
                            json.tags()
                    );
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 从 LLM 回复中提取 JSON 内容。
     */
    private String extractJson(String content) {
        if (content == null || content.isBlank()) {
            return "{}";
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

    /**
     * LLM 返回的分类 JSON 中间 DTO。
     */
    record ClassifiedIssueJson(
            String category,
            String priority,
            String severity,
            String system,
            String assignee,
            java.util.List<String> tags
    ) {}
}
