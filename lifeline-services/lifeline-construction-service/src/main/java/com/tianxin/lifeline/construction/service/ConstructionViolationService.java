package com.tianxin.lifeline.construction.service;

import com.tianxin.lifeline.construction.dto.ViolationReportRequest;
import com.tianxin.lifeline.construction.entity.ConstructionViolation;

import java.util.List;

/**
 * 施工违规业务服务。
 *
 * <p>人工上报（Redis 幂等防重复提交）、AI 视觉命中落库、
 * 违规处理推进；违规产生即经 {@link EvidenceProducer} 投递存证事件。
 */
public interface ConstructionViolationService {

    /**
     * 查询项目下的违规记录清单（创建时间降序）。
     *
     * @param projectId 项目ID
     * @return 违规记录列表
     */
    List<ConstructionViolation> listByProject(Long projectId);

    /**
     * 人工上报违规：落违规记录并投递区块链存证事件，
     * Redis SETNX 幂等拦截窗口内重复提交。
     *
     * @param projectId 项目ID
     * @param request   上报请求
     * @return 违规记录
     */
    ConstructionViolation report(Long projectId, ViolationReportRequest request);

    /**
     * 由 AI 视觉质检命中结果落违规记录并投递存证事件（服务内部调用）。
     *
     * @param projectId   项目ID
     * @param projectName 项目名称
     * @param violationType 违规类型
     * @param description 违规描述
     * @param evidenceUrl 证据影像对象键
     * @param reporter    识别来源标识（如 AI-VISION:摄像头ID）
     * @return 违规记录
     */
    ConstructionViolation createFromVision(Long projectId, String projectName, String violationType,
                                           String description, String evidenceUrl, String reporter);

    /**
     * 处理违规（0 待处理 → 1 已处理，条件更新按影响行数判定）。
     *
     * @param violationId 违规记录ID
     * @return 处理后的违规记录
     */
    ConstructionViolation handle(Long violationId);
}
