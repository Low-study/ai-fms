package com.aifms.config;

import com.aifms.modules.agent.application.IngestSubAgent;
import com.aifms.modules.agent.application.RagSubAgent;
import com.aifms.modules.agent.application.ReportQaSubAgent;
import com.aifms.modules.agent.application.SupervisorAgent;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Agent 模块配置。
 * 暴露 LangChain4j {@link ChatModel} Bean，
 * 并通过 {@link AiServices} 构建 Supervisor Agent 实现。
 *
 * @author aifms
 */
@Configuration
public class AgentConfig {

    private static final Logger log = LoggerFactory.getLogger(AgentConfig.class);

    /**
     * 暴露 LangChain4j ChatModel Bean，供 AiServices 使用。
     * 与 {@code LangChain4jChatModelAdapter} 使用相同的配置参数。
     */
    @Bean
    public ChatModel chatModel(
            @Value("${ai.llm.base-url}") String baseUrl,
            @Value("${ai.llm.api-key}") String apiKey,
            @Value("${ai.llm.model}") String model,
            @Value("${ai.llm.temperature}") double temperature) {
        log.info("创建 ChatModel Bean: baseUrl={}, model={}, temperature={}", baseUrl, model, temperature);
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(model)
                .temperature(temperature)
                .build();
    }

    /**
     * 通过 AiServices 构建 SupervisorAgent 实现。
     * 注册三个子代理为工具，由 Supervisor 的 ChatModel 自主路由调用。
     */
    @Bean
    public SupervisorAgent supervisorAgent(
            ChatModel chatModel,
            IngestSubAgent ingestSubAgent,
            RagSubAgent ragSubAgent,
            ReportQaSubAgent reportQaSubAgent) {
        log.info("构建 SupervisorAgent: 注册 3 个子代理工具");
        return AiServices.builder(SupervisorAgent.class)
                .chatModel(chatModel)
                .tools(ingestSubAgent, ragSubAgent, reportQaSubAgent)
                .build();
    }
}
