package com.aifms.modules.agent.infrastructure;

import com.aifms.modules.agent.domain.DocumentParserPort;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Apache Tika 文档解析器适配器。
 * 使用 Apache Tika 解析 PDF、Word、Excel 等格式的文档数据流，
 * 提取纯文本内容和元数据。
 *
 * @author aifms
 */
@Component
public class TikaDocumentParser implements DocumentParserPort {

    private static final Logger log = LoggerFactory.getLogger(TikaDocumentParser.class);

    @Override
    public Mono<ParsedDocument> parse(Flux<DataBuffer> document) {
        return DataBufferUtils.join(document)
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                })
                .flatMap(bytes -> Mono.fromCallable(() -> parseBytes(bytes))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 使用 Tika 解析字节数组，提取文本和元数据。
     */
    private ParsedDocument parseBytes(byte[] bytes) {
        try (var inputStream = new ByteArrayInputStream(bytes)) {
            BodyContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            AutoDetectParser parser = new AutoDetectParser();

            parser.parse(inputStream, handler, metadata);

            String rawText = handler.toString();
            String contentType = metadata.get("Content-Type");
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            Map<String, Object> metaMap = new HashMap<>();
            for (String name : metadata.names()) {
                metaMap.put(name, metadata.get(name));
            }

            log.debug("Tika 文档解析完成: mimeType={}, textLength={}", contentType, rawText.length());

            return new ParsedDocument(
                    "unknown",
                    contentType,
                    rawText,
                    metaMap
            );
        } catch (Exception e) {
            log.error("Tika 文档解析失败: {}", e.getMessage(), e);
            throw new RuntimeException("文档解析失败: " + e.getMessage(), e);
        }
    }
}
