package com.aifms.modules.agent.infrastructure;

import com.aifms.modules.agent.domain.OcrPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Cloud OCR 适配器（MVP-1 占位实现）。
 * 当前为占位实现，记录告警日志并返回空字符串。
 * 后续版本将对接实际的 OCR 云服务 API。
 *
 * @author aifms
 */
@Component
public class CloudOcrAdapter implements OcrPort {

    private static final Logger log = LoggerFactory.getLogger(CloudOcrAdapter.class);

    private final String provider;

    public CloudOcrAdapter(
            @Value("${ai.ocr.provider}") String provider,
            @Value("${ai.ocr.api-key}") String apiKey) {
        this.provider = provider;

        if ("mock".equalsIgnoreCase(provider)) {
            log.warn("OCR 适配器运行在 mock 模式，不会执行实际 OCR 识别。"
                    + " 设置 OCR_PROVIDER 环境变量以启用真实 OCR 服务。");
        } else {
            log.info("OCR 适配器初始化: provider={}", provider);
        }
    }

    @Override
    public Mono<String> recognize(Flux<DataBuffer> imageData) {
        return imageData
                .then(Mono.fromCallable(() -> {
                    log.warn("OCR 识别请求——当前为占位实现 (provider={})，返回空结果。", provider);
                    return "";
                }));
    }
}
