package com.aifms.modules.agent.domain;

import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 文档解析抽象端口。
 * 将上传的文档（PDF、Word、Excel 等）数据流解析为结构化文本内容。
 * 基础设施层负责对接具体的文档解析引擎（如 Apache Tika、Apache POI、PDFBox）。
 *
 * @author aifms
 */
public interface DocumentParserPort {

    /**
     * 解析文档数据流，提取文本内容和元数据。
     *
     * @param document 文档数据流
     * @return 解析后的文档（包含原始文本和元数据）
     */
    Mono<ParsedDocument> parse(Flux<DataBuffer> document);

    /**
     * 解析后的文档结构。
     */
    record ParsedDocument(
            /** 原始文件名 */
            String originalName,
            /** 文档 MIME 类型（如 application/pdf） */
            String contentType,
            /** 提取的纯文本内容 */
            String rawText,
            /** 文档元数据（如作者、创建时间、页数等） */
            Map<String, Object> metadata
    ) {}
}
