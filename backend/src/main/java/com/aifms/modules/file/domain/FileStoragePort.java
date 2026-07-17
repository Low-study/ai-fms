package com.aifms.modules.file.domain;

import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 文件存储的端口接口（依赖反转）。
 * 定义了文件的存储、加载和删除操作，由基础设施层的适配器实现。
 * 所有方法均为响应式，禁止使用 {@code .block()}。
 */
public interface FileStoragePort {

    /**
     * 将文件内容存储到指定键。
     *
     * @param key         存储键（对象键/路径）
     * @param content     文件内容的数据流
     * @param contentType 文件的 MIME 类型
     * @return 存储完成后的键标识
     */
    Mono<String> store(String key, Flux<DataBuffer> content, String contentType);

    /**
     * 按键加载文件内容。
     *
     * @param key 存储键（对象键/路径）
     * @return 文件内容的数据流
     */
    Flux<DataBuffer> load(String key);

    /**
     * 按键删除文件。
     *
     * @param key 存储键（对象键/路径）
     * @return 删除完成的空信号
     */
    Mono<Void> delete(String key);
}
