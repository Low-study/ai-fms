package com.aifms.modules.finding.infrastructure;

import com.aifms.modules.finding.domain.Finding;
import com.aifms.modules.finding.domain.FindingStatus;

/**
 * 领域对象 {@link Finding} 与持久化实体 {@link FindingEntity} 之间的双向映射器。
 * 所有方法均为纯函数，无状态。
 */
public final class FindingMapper {

    private FindingMapper() {
        // 工具类，禁止实例化
    }

    /**
     * 将持久化实体转换为纯领域对象。
     *
     * @param entity 数据库实体（可为 null）
     * @return 领域对象，entity 为 null 时返回 null
     */
    public static Finding toDomain(FindingEntity entity) {
        if (entity == null) return null;
        Finding finding = new Finding();
        finding.setId(entity.getId());
        finding.setTitle(entity.getTitle());
        finding.setDescription(entity.getDescription());
        finding.setCategory(entity.getCategory());
        finding.setPriority(entity.getPriority());
        finding.setSeverity(entity.getSeverity());
        finding.setSystem(entity.getSystem());
        finding.setAssignee(entity.getAssignee());
        finding.setTags(entity.getTags());
        finding.setStatus(entity.getStatus() != null ? FindingStatus.valueOf(entity.getStatus()) : null);
        finding.setSourceType(entity.getSourceType());
        finding.setSourceFileId(entity.getSourceFileId());
        finding.setTitleJa(entity.getTitleJa());
        finding.setReportDraft(entity.getReportDraft());
        finding.setQaReply(entity.getQaReply());
        finding.setResolution(entity.getResolution());
        finding.setCreatedAt(entity.getCreatedAt());
        finding.setUpdatedAt(entity.getUpdatedAt());
        return finding;
    }

    /**
     * 将领域对象转换为持久化实体。
     *
     * @param finding 领域对象（可为 null）
     * @return 数据库实体，finding 为 null 时返回 null
     */
    public static FindingEntity toEntity(Finding finding) {
        if (finding == null) return null;
        FindingEntity entity = new FindingEntity();
        entity.setId(finding.getId());
        entity.setTitle(finding.getTitle());
        entity.setDescription(finding.getDescription());
        entity.setCategory(finding.getCategory());
        entity.setPriority(finding.getPriority());
        entity.setSeverity(finding.getSeverity());
        entity.setSystem(finding.getSystem());
        entity.setAssignee(finding.getAssignee());
        entity.setTags(finding.getTags());
        entity.setStatus(finding.getStatus() != null ? finding.getStatus().name() : null);
        entity.setSourceType(finding.getSourceType());
        entity.setSourceFileId(finding.getSourceFileId());
        entity.setTitleJa(finding.getTitleJa());
        entity.setReportDraft(finding.getReportDraft());
        entity.setQaReply(finding.getQaReply());
        entity.setResolution(finding.getResolution());
        entity.setCreatedAt(finding.getCreatedAt());
        entity.setUpdatedAt(finding.getUpdatedAt());
        return entity;
    }
}
