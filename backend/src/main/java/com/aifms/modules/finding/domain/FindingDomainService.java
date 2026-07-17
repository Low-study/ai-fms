package com.aifms.modules.finding.domain;

import com.aifms.common.ErrorCodes;
import com.aifms.common.exception.BusinessException;
import org.springframework.stereotype.Service;

/**
 * 指摘领域的跨实体业务规则。
 * 状态机逻辑归 {@link FindingStatus} / {@link Finding} 持有，此处聚焦状态转换校验等横切规则。
 */
@Service
public class FindingDomainService {

    /**
     * 校验状态转换是否合法。
     *
     * @param finding 当前指摘
     * @param target  目标状态
     * @throws BusinessException 40071 状态转换非法
     */
    public void validateStatusTransition(Finding finding, FindingStatus target) {
        if (target != finding.getStatus() && !finding.getStatus().canTransitionTo(target)) {
            throw new BusinessException(ErrorCodes.FINDING_INVALID_STATE,
                    "不允许的状态变更: " + finding.getStatus() + " -> " + target);
        }
    }

    /**
     * 校验标题必填字段。
     *
     * @param title 指摘标题
     * @throws BusinessException 40070 标题不能为空
     */
    public void validateRequiredFields(String title) {
        if (title == null || title.isBlank()) {
            throw new BusinessException(ErrorCodes.FINDING_NOT_FOUND,
                    "指摘标题不能为空");
        }
    }
}
