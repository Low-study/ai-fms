package com.aifms.modules.agent.domain;

import reactor.core.publisher.Mono;

/**
 * 文本嵌入模型抽象端口。
 * 将文本转换为向量表示，用于语义搜索、相似度计算、知识库检索等场景。
 * 基础设施层负责对接具体的嵌入模型（如 text-embedding-3-small、bge-large-zh 等）。
 *
 * @author aifms
 */
public interface EmbeddingModelPort {

    /**
     * 对输入文本进行向量嵌入。
     *
     * @param text 待嵌入的文本
     * @return 浮点数向量（维度由具体模型决定）
     */
    Mono<float[]> embed(String text);
}
