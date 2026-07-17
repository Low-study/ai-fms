package com.aifms.modules.file.presentation;

import com.aifms.common.Result;
import com.aifms.modules.file.application.FileApplicationService;
import com.aifms.modules.file.presentation.dto.FileUploadResponse;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 文件管理的 REST 控制器。
 * 职责单一：校验输入 → 调用 Application Service → 返回 Result&lt;T&gt;。
 * 不包含任何业务逻辑。
 */
@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileApplicationService fileApplicationService;

    public FileController(FileApplicationService fileApplicationService) {
        this.fileApplicationService = fileApplicationService;
    }

    /**
     * 上传单个文件（multipart/form-data）。
     *
     * @param file 上传的文件部件
     * @return 上传成功的文件元数据
     */
    @PostMapping
    public Mono<Result<FileUploadResponse>> upload(@RequestPart("file") FilePart file) {
        return fileApplicationService.upload(file);
    }
}
