package com.aifms.modules.agent.infrastructure;

import com.aifms.modules.agent.domain.McpClientPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * LangChain4j MCP 客户端适配器。
 * 通过 langchain4j-mcp 连接 Model Context Protocol 服务端，
 * 支持列举工具和调用工具。
 * MVP-1 阶段默认连接 filesystem MCP 服务端。
 *
 * @author aifms
 */
@Component
@Lazy
public class LangChain4jMcpClient implements McpClientPort {

    private static final Logger log = LoggerFactory.getLogger(LangChain4jMcpClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final McpClient mcpClient;

    public LangChain4jMcpClient(
            @Value("${ai.mcp.servers.filesystem.command}") String command,
            @Value("${ai.mcp.servers.filesystem.args}") List<String> args) {

        log.info("MCP 客户端初始化: command={}, args={}", command, String.join(" ", args));

        List<String> fullCommand = new java.util.ArrayList<>();
        fullCommand.add(command);
        fullCommand.addAll(args);

        StdioMcpTransport transport = new StdioMcpTransport.Builder()
                .command(fullCommand)
                .build();

        this.mcpClient = new DefaultMcpClient.Builder()
                .transport(transport)
                .build();
    }

    @Override
    public Flux<McpTool> listTools() {
        return Mono.fromCallable(() -> mcpClient.listTools().stream()
                        .map(tool -> new McpTool(
                                tool.name(),
                                tool.description() != null ? tool.description() : "",
                                convertParameters(tool.parameters())
                        ))
                        .collect(Collectors.toList()))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable);
    }

    @Override
    public Mono<McpToolResult> invokeTool(String toolName, Map<String, Object> args) {
        return Mono.fromCallable(() -> {
            try {
                String argumentsJson;
                try {
                    argumentsJson = objectMapper.writeValueAsString(args);
                } catch (JsonProcessingException e) {
                    argumentsJson = "{}";
                }

                ToolExecutionRequest request = ToolExecutionRequest.builder()
                        .name(toolName)
                        .arguments(argumentsJson)
                        .build();

                String result = mcpClient.executeTool(request);

                return new McpToolResult(result, null);
            } catch (Exception e) {
                log.error("MCP 工具调用失败: tool={}, error={}", toolName, e.getMessage());
                return new McpToolResult("", e.getMessage());
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 将 ToolSpecification 的 parameters 转换为 Map。
     */
    private Map<String, Object> convertParameters(Object parameters) {
        if (parameters == null) {
            return Map.of();
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.convertValue(parameters, Map.class);
            return result;
        } catch (Exception e) {
            return Map.of();
        }
    }
}
