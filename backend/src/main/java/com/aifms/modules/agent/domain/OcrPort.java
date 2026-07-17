package com.aifms.modules.agent.domain;

import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * OCR（光学字符识别）抽象端口。
 * 从图片数据流中提取文字内容。
 * 基础设施层负责对接具体的 OCR 引擎（如 Tesseract、PaddleOCR、云服务 OCR API）。
 *
 * @author aifms
 */
public interface OcrPort {

    /**
     * 对图片数据流进行文字识别。
     *
     * @param imageData 图片数据流（支持 PNG、JPEG、TIFF 等格式）
     * @return 识别出的文字内容
     */
    Mono<String> recognize(Flux<DataBuffer> imageData);
}
