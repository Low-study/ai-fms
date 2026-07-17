package com.aifms.modules.agent.infrastructure;

import com.aifms.modules.agent.domain.ChatModelPort;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.TokenUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * LangChain4j ChatModel 适配器。
 * 将 LangChain4j 的 OpenAiChatModel 适配为 ChatModelPort 接口，
 * 默认指向 DeepSeek API，兼容所有 OpenAI 兼容端点。
 *
 * @author aifms
 */
@Component
public class LangChain4jChatModelAdapter implements ChatModelPort {

    private static final Logger log = LoggerFactory.getLogger(LangChain4jChatModelAdapter.class);

    private final OpenAiChatModel chatModel;
    private final OpenAiStreamingChatModel streamingChatModel;

    public LangChain4jChatModelAdapter(
            @Value("${ai.llm.base-url}") String baseUrl,
            @Value("${ai.llm.api-key}") String apiKey,
            @Value("${ai.llm.model}") String model,
            @Value("${ai.llm.temperature}") double temperature) {

        this.chatModel = OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(model)
                .temperature(temperature)
                .build();

        this.streamingChatModel = OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(model)
                .temperature(temperature)
                .build();

        log.info("LLM ChatModel 初始化完成: baseUrl={}, model={}, temperature={}", baseUrl, model, temperature);
    }

    @Override
    public Mono<ChatResponse> call(ChatRequest request) {
        return Mono.fromCallable(() -> {
            dev.langchain4j.model.chat.request.ChatRequest lcRequest =
                    dev.langchain4j.model.chat.request.ChatRequest.builder()
                            .messages(List.of(
                                    SystemMessage.systemMessage(request.systemPrompt()),
                                    UserMessage.from(request.userPrompt())
                            ))
                            .build();

            dev.langchain4j.model.chat.response.ChatResponse lcResponse = chatModel.chat(lcRequest);

            AiMessage aiMessage = lcResponse.aiMessage();
            TokenUsage tokenUsage = lcResponse.tokenUsage();
            int tokensUsed = tokenUsage != null ? tokenUsage.totalTokenCount() : 0;

            return new ChatResponse(
                    aiMessage.text(),
                    tokensUsed,
                    0.0
            );
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Flux<ChatChunk> stream(ChatRequest request) {
        return Mono.<Flux<ChatChunk>>fromCallable(() -> {
            Sinks.Many<ChatChunk> sink = Sinks.many().unicast().onBackpressureBuffer();

            dev.langchain4j.model.chat.request.ChatRequest lcRequest =
                    dev.langchain4j.model.chat.request.ChatRequest.builder()
                            .messages(List.of(
                                    SystemMessage.systemMessage(request.systemPrompt()),
                                    UserMessage.from(request.userPrompt())
                            ))
                            .build();

            streamingChatModel.chat(lcRequest, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String partialResponse) {
                    sink.tryEmitNext(new ChatChunk(partialResponse, null));
                }

                @Override
                public void onCompleteResponse(
                        dev.langchain4j.model.chat.response.ChatResponse completeResponse) {
                    sink.tryEmitComplete();
                }

                @Override
                public void onError(Throwable error) {
                    log.error("LLM 流式调用出错: {}", error.getMessage(), error);
                    sink.tryEmitError(error);
                }
            });

            return sink.asFlux();
        }).flatMapMany(flux -> flux)
          .subscribeOn(Schedulers.boundedElastic());
    }
}
