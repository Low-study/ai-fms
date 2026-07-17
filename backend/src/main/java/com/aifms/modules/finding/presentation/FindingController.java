package com.aifms.modules.finding.presentation;

import com.aifms.common.Result;
import com.aifms.common.dto.PageResult;
import com.aifms.modules.agent.domain.KnowledgeRagSkill;
import com.aifms.modules.agent.domain.ParsedIssue;
import com.aifms.modules.agent.domain.SimilarIssues;
import com.aifms.modules.finding.application.FindingApplicationService;
import com.aifms.modules.finding.presentation.dto.CreateFindingRequest;
import com.aifms.modules.finding.presentation.dto.FindingResponse;
import com.aifms.modules.finding.presentation.dto.UpdateFindingRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 指摘管理的 REST 控制器。
 * 职责单一：校验输入 → 调用 Application Service → 返回 Result&lt;T&gt;。
 * 不包含任何业务逻辑。
 */
@Validated
@RestController
@RequestMapping("/api/v1/findings")
public class FindingController {

    private final FindingApplicationService findingApplicationService;
    private final KnowledgeRagSkill knowledgeRagSkill;

    public FindingController(FindingApplicationService findingApplicationService,
                             KnowledgeRagSkill knowledgeRagSkill) {
        this.findingApplicationService = findingApplicationService;
        this.knowledgeRagSkill = knowledgeRagSkill;
    }

    /**
     * 分页搜索指摘列表。
     *
     * @param keyword 搜索关键字（可选）
     * @param page    页码（默认 1）
     * @param size    每页大小（默认 20）
     */
    @GetMapping
    public Mono<Result<PageResult<FindingResponse>>> list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须为正整数") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页大小至少为 1")
                                                @Max(value = 100, message = "每页大小最多为 100") int size) {
        return findingApplicationService.listFindings(keyword, page, size);
    }

    /**
     * 按 ID 获取指摘详情。
     *
     * @param id 指摘 ID
     */
    @GetMapping("/{id}")
    public Mono<Result<FindingResponse>> get(@PathVariable UUID id) {
        return findingApplicationService.getById(id);
    }

    /**
     * 创建新指摘。
     *
     * @param request 创建请求体（含校验注解）
     */
    @PostMapping
    public Mono<Result<FindingResponse>> create(@Valid @RequestBody CreateFindingRequest request) {
        return findingApplicationService.create(request);
    }

    /**
     * 全量更新指摘信息（PUT）。
     *
     * @param id      指摘 ID
     * @param request 更新请求体（仅更新传入的非空字段）
     */
    @PutMapping("/{id}")
    public Mono<Result<FindingResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFindingRequest request) {
        return findingApplicationService.update(id, request);
    }

    /**
     * 关闭指摘（软删除）。
     *
     * @param id 指摘 ID
     */
    @DeleteMapping("/{id}")
    public Mono<Result<Void>> delete(@PathVariable UUID id) {
        return findingApplicationService.delete(id);
    }

    /**
     * 按指摘 ID 检索相似历史案例（RAG）。
     */
    @GetMapping("/{id}/similar")
    public Mono<Result<SimilarIssues>> getSimilar(@PathVariable UUID id) {
        return findingApplicationService.getById(id)
                .map(Result::getData)
                .flatMap(fr -> {
                    ParsedIssue parsed = new ParsedIssue(
                        fr.getTitle() != null ? fr.getTitle() : "",
                        fr.getDescription() != null ? fr.getDescription() : "",
                        "");
                    return knowledgeRagSkill.retrieveSimilar(id, parsed);
                })
                .map(Result::success);
    }
}
