package com.tianxin.lifeline.warning.service;

import com.tianxin.lifeline.api.warning.dto.WarningEventDTO;
import com.tianxin.lifeline.common.core.page.PageResult;
import com.tianxin.lifeline.warning.dto.WarningEventPayload;
import com.tianxin.lifeline.warning.dto.WarningEventVO;
import com.tianxin.lifeline.warning.dto.WarningQueryRequest;
import com.tianxin.lifeline.warning.dto.WarningStatisticsVO;
import com.tianxin.lifeline.warning.entity.WarningEvent;

import java.math.BigDecimal;
import java.util.List;

/**
 * 预警事件业务服务。
 *
 * <p>统一收口预警事件的创建（REST 人工/契约创建与 Kafka 规则创建共用
 * 同一幂等落库逻辑）、查询、状态机流转（确认/核销）与统计。
 */
public interface WarningEventService {

    /**
     * 分页查询预警事件（状态输出中文）。
     *
     * @param request 分页与过滤条件
     * @return 分页结果
     */
    PageResult<WarningEventVO> page(WarningQueryRequest request);

    /**
     * 查询预警事件详情。
     *
     * @param warningId 预警事件ID
     * @return 预警事件 DTO，不存在抛出业务异常
     */
    WarningEventDTO getById(Long warningId);

    /**
     * 查询指定管网的活跃预警（状态为待确认/已确认/处置中）。
     *
     * @param pipeId 管网ID
     * @return 活跃预警列表
     */
    List<WarningEventDTO> listActive(Long pipeId);

    /**
     * 查询指定区域的活跃预警（状态为待确认/已确认/处置中，按 area_id 过滤）。
     *
     * @param areaId 区域ID
     * @return 活跃预警列表
     */
    List<WarningEventDTO> listActiveByArea(Long areaId);

    /**
     * 创建预警事件（REST 入口，走统一幂等落库逻辑）。
     *
     * @param dto 预警事件 DTO
     * @return 创建后的预警事件（含ID）
     */
    WarningEventDTO create(WarningEventDTO dto);

    /**
     * 创建预警事件（Kafka 入口，幂等落库）。
     *
     * @param payload 预警事件负载
     * @param aiScore AI 异常评分，纯规则判定传 null
     * @return 落库后的实体；warningNo 重复时返回 null
     */
    WarningEvent createFromPayload(WarningEventPayload payload, BigDecimal aiScore);

    /**
     * 确认预警（状态机：0 待确认 → 1 已确认）。
     *
     * @param warningId 预警事件ID
     * @return 更新后的预警事件 DTO
     */
    WarningEventDTO confirm(Long warningId);

    /**
     * 核销预警（状态机：未核销状态 → 3 已核销，写 resolved_at）。
     *
     * @param warningId 预警事件ID
     * @return 更新后的预警事件 DTO
     */
    WarningEventDTO cancel(Long warningId);

    /**
     * 按等级/管网类型/状态三维统计预警事件。
     *
     * @return 统计结果
     */
    WarningStatisticsVO statistics();
}
