package com.aifms.modules.agent.infrastructure;

import com.aifms.modules.agent.domain.ChatModelPort;
import com.aifms.modules.agent.domain.ClassifiedIssue;
import com.aifms.modules.agent.domain.IssueClassifySkill;
import com.aifms.modules.agent.domain.LanguageGuard;
import com.aifms.modules.agent.domain.ParsedIssue;
import com.aifms.modules.agent.domain.PromptTemplatePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(IssueClassifySkillAdapter.class);
    private static final String TEMPLATE_NAME = "issue_classify_skill";
    private static final int TEMPLATE_VERSION = 1;
    private static final int MAX_RETRIES = 2;

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
        String expectedLang = LanguageGuard.detect(issue.title());
        String userPrompt = "Title: " + issue.title() + "\nDescription: " + issue.description();
        return promptTemplatePort.findByNameVersion(TEMPLATE_NAME, TEMPLATE_VERSION)
                .flatMap(template -> callAndCheck(template.systemTemplate(), userPrompt, expectedLang, 0))
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

    private Mono<ChatModelPort.ChatResponse> callAndCheck(String sysPrompt, String userPrompt,
                                                          String expectedLang, int retryCount) {
        String effectiveSys = sysPrompt;
        if (retryCount > 0) {
            effectiveSys = sysPrompt + LanguageGuard.retryEmphasis(expectedLang);
            log.warn("Classify lang mismatch, retry {}/{}: expected={}", retryCount, MAX_RETRIES, expectedLang);
        }
        return chatModelPort.call(new ChatModelPort.ChatRequest(effectiveSys, userPrompt, null, 0.3))
                .flatMap(resp -> {
                    try {
                        ClassifiedIssueJson json = objectMapper.readValue(extractJson(resp.content()), ClassifiedIssueJson.class);
                        String outLang = LanguageGuard.detect(json.priority() != null ? json.priority() : "");
                        if (!outLang.equals(expectedLang) && retryCount < MAX_RETRIES) {
                            return callAndCheck(sysPrompt, userPrompt, expectedLang, retryCount + 1);
                        }
                    } catch (Exception ignored) {}
                    return Mono.just(resp);
                });
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
