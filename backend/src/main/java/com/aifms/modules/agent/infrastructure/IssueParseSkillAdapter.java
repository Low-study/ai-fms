package com.aifms.modules.agent.infrastructure;

import com.aifms.modules.agent.domain.ChatModelPort;
import com.aifms.modules.agent.domain.DocumentParserPort;
import com.aifms.modules.agent.domain.IssueParseSkill;
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
 * 工单解析技能适配器。
 * 通过 ChatModelPort 调用 LLM，从文档解析结果中提取工单结构化信息。
 *
 * @author aifms
 */
@Service
public class IssueParseSkillAdapter implements IssueParseSkill {

    private static final Logger log = LoggerFactory.getLogger(IssueParseSkillAdapter.class);
    private static final String TEMPLATE_NAME = "issue_parse_skill";
    private static final int TEMPLATE_VERSION = 1;
    private static final String DEFAULT_MODEL = "deepseek-chat";
    private static final double DEFAULT_TEMPERATURE = 0.3;
    private static final int MAX_RETRIES = 2;

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
        String inputLang = LanguageGuard.detect(document.rawText());
        return promptTemplatePort.findByNameVersion(TEMPLATE_NAME, TEMPLATE_VERSION)
                .flatMap(template -> callAndParse(template.systemTemplate(),
                        template.userTemplate() + "\n\n" + document.rawText(), inputLang, 0))
                .flatMap(response -> Mono.fromCallable(() ->
                        objectMapper.readValue(extractJson(response.content()), ParsedIssue.class)
                ).subscribeOn(Schedulers.boundedElastic()));
    }

    private Mono<ChatModelPort.ChatResponse> callAndParse(String sysPrompt, String userPrompt,
                                                          String expectedLang, int retryCount) {
        String effectiveSys = sysPrompt;
        if (retryCount > 0) {
            effectiveSys = sysPrompt + LanguageGuard.retryEmphasis(expectedLang);
            log.warn("Parse language mismatch, retry {}/{}: expected={}", retryCount, MAX_RETRIES, expectedLang);
        }
        ChatModelPort.ChatRequest req = new ChatModelPort.ChatRequest(
                effectiveSys, userPrompt, DEFAULT_MODEL, DEFAULT_TEMPERATURE);
        return chatModelPort.call(req).flatMap(resp -> {
            String json = extractJson(resp.content());
            try {
                ParsedIssue pi = objectMapper.readValue(json, ParsedIssue.class);
                String outLang = LanguageGuard.detect(pi.title());
                if (!outLang.equals(expectedLang) && retryCount < MAX_RETRIES) {
                    return callAndParse(sysPrompt, userPrompt, expectedLang, retryCount + 1);
                }
                return Mono.just(resp);
            } catch (Exception e) {
                if (retryCount < MAX_RETRIES) {
                    return callAndParse(sysPrompt, userPrompt, expectedLang, retryCount + 1);
                }
                return Mono.just(resp);
            }
        });
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
