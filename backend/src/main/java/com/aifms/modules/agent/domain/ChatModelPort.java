package com.aifms.modules.agent.domain;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * LLM 调用抽象端口。
 * 定义与大型语言模型交互的统一契约，提供同步调用与流式调用两种模式。
 * 基础设施层负责对接具体模型供应商（如 OpenAI、Claude、本地模型等）。
 *
 * @author aifms
 */
public interface ChatModelPort {

    /**
     * 同步调用 LLM，返回完整回复。
     *
     * @param request 聊天请求参数
     * @return 聊天响应（包含回复内容、Token 用量和成本）
     */
    Mono<ChatResponse> call(ChatRequest request);

    /**
     * 流式调用 LLM，逐步返回回复片段。
     * 适用于需要实时展示生成内容的场景。
     *
     * @param request 聊天请求参数
     * @return 聊天片段流，每个元素为一个增量内容块
     */
    Flux<ChatChunk> stream(ChatRequest request);

    /**
     * 聊天请求参数。
     * 封装一次 LLM 调用所需的全部输入。
     */
    record ChatRequest(
            /** 系统提示词（设定助手角色和行为） */
            String systemPrompt,
            /** 用户提示词（具体的用户问题或指令） */
            String userPrompt,
            /** 模型标识（如 gpt-4o、claude-3.5-sonnet） */
            String model,
            /** 温度参数（0.0~2.0，控制输出随机性） */
            double temperature
    ) {}

    /**
     * 聊天响应结果。
     * 封装 LLM 同步调用的返回信息。
     */
    record ChatResponse(
            /** 模型生成的回复内容 */
            String content,
            /** 消耗的 Token 总数 */
            int tokensUsed,
            /** 调用成本（美元） */
            double cost
    ) {}

    /**
     * 流式聊天输出片段。
     * 流式调用时每个增量内容块对应一个 ChatChunk。
     */
    record ChatChunk(
            /** 增量文本内容 */
            String content,
            /** 结束原因（如 "stop"、"length"、"tool_calls"），null 表示尚未结束 */
            String finishReason
    ) {}
}
