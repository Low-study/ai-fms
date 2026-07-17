package com.aifms.modules.finding.application;

import com.aifms.common.ErrorCodes;
import com.aifms.common.Result;
import com.aifms.common.dto.PageResult;
import com.aifms.common.exception.BusinessException;
import com.aifms.modules.finding.domain.Finding;
import com.aifms.modules.finding.domain.FindingDomainService;
import com.aifms.modules.finding.domain.FindingStatus;
import com.aifms.modules.finding.infrastructure.FindingMapper;
import com.aifms.modules.finding.infrastructure.FindingRepository;
import com.aifms.modules.finding.presentation.dto.CreateFindingRequest;
import com.aifms.modules.finding.presentation.dto.FindingResponse;
import com.aifms.modules.finding.presentation.dto.UpdateFindingRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 指摘管理的应用服务层。
 * 负责用例编排：校验 → 领域操作 → 持久化 → 返回 DTO。
 * 全链路响应式，禁止使用 {@code .block()}。
 */
@Service
public class FindingApplicationService {

    private final FindingRepository findingRepository;
    private final FindingDomainService findingDomainService;
    private final TransactionalOperator transactionalOperator;

    public FindingApplicationService(FindingRepository findingRepository,
                                     FindingDomainService findingDomainService,
                                     TransactionalOperator transactionalOperator) {
        this.findingRepository = findingRepository;
        this.findingDomainService = findingDomainService;
        this.transactionalOperator = transactionalOperator;
    }

    // ── 分页列表 ──

    /**
     * 分页查询非关闭指摘，支持关键字模糊搜索。
     *
     * @param keyword 搜索关键字（可空）
     * @param page    页码（从 1 开始）
     * @param size    每页大小
     * @return 分页结果
     */
    public Mono<Result<PageResult<FindingResponse>>> listFindings(String keyword, int page, int size) {
        String k = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        long offset = (long) (page - 1) * size;

        return findingRepository.countNonClosed(k)
                .flatMap(total -> findingRepository.findAllNonClosed(k, size, offset)
                        .map(FindingMapper::toDomain)
                        .map(FindingResponse::from)
                        .collectList()
                        .map(items -> {
                            PageResult<FindingResponse> pageResult = new PageResult<>(items, page, size, total);
                            return Result.success(pageResult);
                        }));
    }

    // ── 按 ID 查询 ──

    /**
     * 按 ID 查询单个指摘。
     *
     * @param id 指摘 ID
     * @return 指摘响应
     * @throws BusinessException 40070 指摘不存在或已关闭
     */
    public Mono<Result<FindingResponse>> getById(UUID id) {
        return findingRepository.findByIdNonClosed(id)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCodes.FINDING_NOT_FOUND,
                        "指摘不存在: " + id)))
                .map(FindingMapper::toDomain)
                .map(FindingResponse::from)
                .map(Result::success);
    }

    // ── 创建 ──

    /**
     * 创建新指摘。
     * 校验标题唯一性后持久化。
     *
     * @param request 创建请求体
     * @return 创建成功的指摘响应
     * @throws BusinessException 40072 标题重复
     */
    public Mono<Result<FindingResponse>> create(CreateFindingRequest request) {
        findingDomainService.validateRequiredFields(request.getTitle());

        return findingRepository.findByTitleNonClosed(request.getTitle())
                .flatMap(existing -> Mono.<FindingResponse>error(
                        new BusinessException(ErrorCodes.FINDING_DUPLICATE,
                                "指摘标题已存在: " + request.getTitle())))
                .cast(FindingResponse.class)
                .switchIfEmpty(Mono.defer(() -> {
                    Finding finding = Finding.create(
                            request.getTitle(),
                            request.getDescription(),
                            request.getCategory(),
                            request.getPriority(),
                            request.getSeverity(),
                            request.getSystem(),
                            request.getAssignee(),
                            request.getTags(),
                            request.getSourceType(),
                            request.getSourceFileId(),
                            request.getTitleJa()
                    );
                    return findingRepository.save(FindingMapper.toEntity(finding))
                            .map(FindingMapper::toDomain)
                            .map(FindingResponse::from);
                }))
                .map(Result::success)
                .as(transactionalOperator::transactional);
    }

    // ── 全量更新 ──

    /**
     * 全量更新指摘信息（diff-based）。
     * 仅更新传入的非空字段。
     *
     * @param id      指摘 ID
     * @param request 更新请求体
     * @return 更新后的指摘响应
     * @throws BusinessException 40070 指摘不存在 / 40072 标题冲突 / 40071 状态转换非法
     */
    public Mono<Result<FindingResponse>> update(UUID id, UpdateFindingRequest request) {
        return findingRepository.findByIdNonClosed(id)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCodes.FINDING_NOT_FOUND,
                        "指摘不存在: " + id)))
                .map(FindingMapper::toDomain)
                .flatMap(finding -> {
                    // 标题唯一性校验（仅在变更时）
                    if (request.getTitle() != null && !request.getTitle().equals(finding.getTitle())) {
                        return findingRepository.findByTitleNonClosed(request.getTitle())
                                .flatMap(existing -> Mono.<Finding>error(
                                        new BusinessException(ErrorCodes.FINDING_DUPLICATE,
                                                "指摘标题已存在: " + request.getTitle())))
                                .thenReturn(finding);
                    }
                    return Mono.just(finding);
                })
                .flatMap(finding -> {
                    if (request.getTitle() != null) finding.setTitle(request.getTitle());
                    if (request.getDescription() != null) finding.setDescription(request.getDescription());
                    if (request.getCategory() != null) finding.setCategory(request.getCategory());
                    if (request.getPriority() != null) finding.setPriority(request.getPriority());
                    if (request.getSeverity() != null) finding.setSeverity(request.getSeverity());
                    if (request.getSystem() != null) finding.setSystem(request.getSystem());
                    if (request.getAssignee() != null) finding.setAssignee(request.getAssignee());
                    if (request.getTags() != null) finding.setTags(request.getTags());
                    if (request.getSourceType() != null) finding.setSourceType(request.getSourceType());
                    if (request.getSourceFileId() != null) finding.setSourceFileId(request.getSourceFileId());
                    if (request.getTitleJa() != null) finding.setTitleJa(request.getTitleJa());
                    if (request.getReportDraft() != null) finding.setReportDraft(request.getReportDraft());
                    if (request.getQaReply() != null) finding.setQaReply(request.getQaReply());
                    if (request.getResolution() != null) finding.setResolution(request.getResolution());
                    // 状态变更通过状态机校验
                    if (request.getStatus() != null) {
                        FindingStatus target = FindingStatus.valueOf(request.getStatus().toUpperCase());
                        findingDomainService.validateStatusTransition(finding, target);
                        if (target != finding.getStatus()) {
                            finding.changeStatus(target);
                        }
                    }
                    // 更新时间戳
                    finding.setUpdatedAt(java.time.Instant.now());
                    return findingRepository.save(FindingMapper.toEntity(finding));
                })
                .map(FindingMapper::toDomain)
                .map(FindingResponse::from)
                .map(Result::success)
                .as(transactionalOperator::transactional);
    }

    // ── 软删除（关闭） ──

    /**
     * 关闭指摘（将状态设为 CLOSED 终态）。
     *
     * @param id 指摘 ID
     * @return 空结果
     * @throws BusinessException 40070 指摘不存在或已关闭
     */
    public Mono<Result<Void>> delete(UUID id) {
        return findingRepository.findByIdNonClosed(id)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCodes.FINDING_NOT_FOUND,
                        "指摘不存在: " + id)))
                .map(FindingMapper::toDomain)
                .flatMap(finding -> {
                    finding.close();
                    return findingRepository.save(FindingMapper.toEntity(finding));
                })
                .thenReturn(Result.<Void>success())
                .as(transactionalOperator::transactional);
    }
}
