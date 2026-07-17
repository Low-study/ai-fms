package com.aifms.modules.agent.domain;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * MCP（Model Context Protocol）客户端抽象端口。
 * 与外部 MCP 服务通信，列举可用工具并调用指定工具。
 * 基础设施层负责对接具体的 MCP 传输协议（如 stdio、SSE、WebSocket）。
 *
 * @author aifms
 */
public interface McpClientPort {

    /**
     * 列举 MCP 服务端提供的所有工具。
     *
     * @return 工具列表流
     */
    Flux<McpTool> listTools();

    /**
     * 调用指定的 MCP 工具。
     *
     * @param toolName 工具名称
     * @param args     工具调用参数
     * @return 工具执行结果
     */
    Mono<McpToolResult> invokeTool(String toolName, Map<String, Object> args);

    /**
     * MCP 工具描述。
     */
    record McpTool(
            /** 工具名称 */
            String name,
            /** 工具功能描述 */
            String description,
            /** 工具参数 JSON Schema */
            Map<String, Object> parametersSchema
    ) {}

    /**
     * MCP 工具调用结果。
     */
    record McpToolResult(
            /** 工具返回的内容 */
            String content,
            /** 错误信息（可空，调用成功时为 null） */
            String error
    ) {}
}
