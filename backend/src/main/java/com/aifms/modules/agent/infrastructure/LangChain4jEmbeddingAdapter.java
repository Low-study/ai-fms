package com.aifms.modules.agent.infrastructure;

import com.aifms.modules.agent.domain.EmbeddingModelPort;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.time.Duration;

/**
 * LangChain4j Embedding 模型适配器。
 * 将 LangChain4j 的 OpenAiEmbeddingModel 适配为 EmbeddingModelPort 接口，
 * 默认使用 text-embedding-3-large，兼容所有 OpenAI 兼容端点。
 *
 * @author aifms
 */
@Component
public class LangChain4jEmbeddingAdapter implements EmbeddingModelPort {

    private static final Logger log = LoggerFactory.getLogger(LangChain4jEmbeddingAdapter.class);

    private final EmbeddingModel embeddingModel;

    public LangChain4jEmbeddingAdapter(
            @Value("${ai.embedding.base-url}") String baseUrl,
            @Value("${ai.embedding.api-key}") String apiKey,
            @Value("${ai.embedding.model}") String model) {

        this.embeddingModel = OpenAiEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(model)
                .build();

        log.info("Embedding 模型初始化完成: baseUrl={}, model={}", baseUrl, model);
    }

    @Override
    public Mono<float[]> embed(String text) {
        return Mono.fromCallable(() -> {
            dev.langchain4j.model.output.Response<Embedding> response =
                    embeddingModel.embed(TextSegment.from(text));

            return response.content().vector();
        }).subscribeOn(Schedulers.boundedElastic())
                .timeout(Duration.ofSeconds(10));
    }
}
