package com.aifms.modules.agent.infrastructure;

import com.aifms.modules.agent.domain.ChatModelPort;
import com.aifms.modules.agent.domain.ClassifiedIssue;
import com.aifms.modules.agent.domain.LanguageGuard;
import com.aifms.modules.agent.domain.PromptTemplatePort;
import com.aifms.modules.agent.domain.ReportDraftSkill;
import com.aifms.modules.agent.domain.SimilarIssues;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(ReportDraftSkillAdapter.class);
    private static final String TEMPLATE_NAME = "report_draft_skill";
    private static final int TEMPLATE_VERSION = 1;
    private static final int MAX_RETRIES = 2;

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
        String expectedLang = LanguageGuard.detect(issue.issue().title());
        return promptTemplatePort.findByNameVersion(TEMPLATE_NAME, TEMPLATE_VERSION)
                .flatMap(template -> {
                    String userPrompt = template.userTemplate()
                            + "\n\nIssue:\n" + toJson(issue)
                            + "\n\nSimilar issues:\n" + toJson(similar);
                    return callAndCheck(template.systemTemplate(), userPrompt, expectedLang, 0);
                })
                .flatMap(resp -> Mono.fromCallable(() -> {
                    try {
                        return objectMapper.readValue(extractJson(resp.content()), ReportJson.class).report();
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
            log.warn("Report lang mismatch, retry {}/{}: expected={}", retryCount, MAX_RETRIES, expectedLang);
        }
        return chatModelPort.call(new ChatModelPort.ChatRequest(effectiveSys, userPrompt, null, 0.3))
                .flatMap(resp -> {
                    String text = extractJson(resp.content());
                    try { text = objectMapper.readValue(text, ReportJson.class).report(); } catch (Exception ignored) {}
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
