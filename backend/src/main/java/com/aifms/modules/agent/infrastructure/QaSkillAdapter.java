package com.aifms.modules.agent.infrastructure;

import com.aifms.modules.agent.domain.ChatModelPort;
import com.aifms.modules.agent.domain.ClassifiedIssue;
import com.aifms.modules.agent.domain.LanguageGuard;
import com.aifms.modules.agent.domain.PromptTemplatePort;
import com.aifms.modules.agent.domain.QaSkill;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 智能问答技能适配器。
 * 通过 ChatModelPort 调用 LLM，基于分类工单生成智能回复内容。
 *
 * @author aifms
 */
@Service
public class QaSkillAdapter implements QaSkill {

    private static final String TEMPLATE_NAME = "qa_skill";
    private static final int TEMPLATE_VERSION = 1;
    private static final String DEFAULT_MODEL = "gpt-4o";
    private static final double DEFAULT_TEMPERATURE = 0.7;
    private static final int MAX_RETRIES = 2;

    private static final Logger log = LoggerFactory.getLogger(QaSkillAdapter.class);

    private final ChatModelPort chatModelPort;
    private final PromptTemplatePort promptTemplatePort;
    private final ObjectMapper objectMapper;

    public QaSkillAdapter(ChatModelPort chatModelPort,
                          PromptTemplatePort promptTemplatePort,
                          ObjectMapper objectMapper) {
        this.chatModelPort = chatModelPort;
        this.promptTemplatePort = promptTemplatePort;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<String> generateReply(ClassifiedIssue issue) {
        String expectedLang = LanguageGuard.detect(issue.issue().title());
        String userPrompt = "Issue:\n"
                + "Title: " + issue.issue().title() + "\n"
                + "Description: " + issue.issue().description() + "\n"
                + "Category: " + issue.category() + "\n"
                + "Priority: " + issue.priority() + "\n"
                + "Severity: " + issue.severity();
        return promptTemplatePort.findByNameVersion(TEMPLATE_NAME, TEMPLATE_VERSION)
                .flatMap(template -> callAndCheck(template.systemTemplate(), userPrompt, expectedLang, 0))
                .flatMap(resp -> Mono.fromCallable(() -> {
                    try {
                        return objectMapper.readValue(extractJson(resp.content()), QaReplyJson.class).reply();
                    } catch (JsonProcessingException e) {
                        return extractJson(resp.content());
                    }
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    private Mono<ChatModelPort.ChatResponse> callAndCheck(String sysPrompt, String userPrompt,
                                                          String expectedLang, int retryCount) {
        String effectiveSys = sysPrompt;
        if (retryCount > 0) {
            effectiveSys = sysPrompt + LanguageGuard.retryEmphasis(expectedLang);
            log.warn("QA language mismatch, retry {}/{}: expected={}", retryCount, MAX_RETRIES, expectedLang);
        }
        return chatModelPort.call(new ChatModelPort.ChatRequest(
                        effectiveSys, userPrompt, DEFAULT_MODEL, DEFAULT_TEMPERATURE))
                .flatMap(resp -> {
                    String text = extractJson(resp.content());
                    try { text = objectMapper.readValue(text, QaReplyJson.class).reply(); } catch (Exception ignored) {}
                    String outLang = LanguageGuard.detect(text);
                    if (!outLang.equals(expectedLang) && retryCount < MAX_RETRIES) {
                        return callAndCheck(sysPrompt, userPrompt, expectedLang, retryCount + 1);
                    }
                    return Mono.just(resp);
                });
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

    /**
     * LLM 返回的智能问答 JSON 中间 DTO。
     */
    record QaReplyJson(String reply) {}
}
