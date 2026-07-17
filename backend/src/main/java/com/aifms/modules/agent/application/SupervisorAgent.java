package com.aifms.modules.agent.application;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Supervisor Agent 接口。
 * 由 LangChain4j {@link dev.langchain4j.service.AiServices} 生成实现，
 * 通过内部 ChatModel 路由到 IngestSubAgent、RagSubAgent、ReportQaSubAgent 三个子代理。
 *
 * <p>处理流程：
 * <ol>
 *   <li>调用 parseAndClassify 解析并分类工单原始文本</li>
 *   <li>调用 retrieveSimilar 检索相似历史工单</li>
 *   <li>调用 draftReport 起草处理报告</li>
 *   <li>调用 generateReply 生成智能回复</li>
 * </ol>
 *
 * @author aifms
 */
public interface SupervisorAgent {

    /**
     * 执行完整的工单处理流水线。
     * Supervisor 的 ChatModel 根据任务描述自主决定调用子代理的顺序和参数，
     * 最终汇总所有子代理的输出作为处理结果。
     *
     * @param taskDescription 任务描述（包含原始文本和文件名等上下文）
     * @return 处理结果摘要
     */
    @SystemMessage("""
            You are a supervisor agent for an AI quality inspection finding management system.
            Your job is to process a finding document through a multi-step pipeline using the available tools.

            Available tools:
            - parseAndClassify(rawText, originalName): Parse raw document text and classify the issue
            - retrieveSimilar(title, description): Retrieve similar historical issues from knowledge base
            - draftReport(ingestResultJson, similarJson): Generate a processing report draft
            - generateReply(ingestResultJson): Generate an intelligent QA reply

            Process each finding by calling the tools in order:
            1. First, call parseAndClassify with the raw text and original filename
            2. Then, call retrieveSimilar with the title and description from step 1
            3. Next, call draftReport with the result from step 1 as ingestResult and step 2 as similar results
            4. Finally, call generateReply with the result from step 1

            Return a concise summary with: title, category, priority, severity, and whether report/QA were generated.
            """)
    String process(@UserMessage String taskDescription);
}
