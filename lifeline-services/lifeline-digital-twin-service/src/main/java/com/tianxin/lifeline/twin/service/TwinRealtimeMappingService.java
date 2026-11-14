package com.tianxin.lifeline.twin.service;

import com.tianxin.lifeline.twin.dto.RealtimeMappingRequest;
import com.tianxin.lifeline.twin.dto.RealtimeMappingVO;

/**
 * 实时映射服务。
 *
 * <p>按 deviceId 列表或 areaId 解析目标设备，Redis Pipeline 批量读取
 * {@code rt:{deviceId}} HASH 拼装设备实时状态数组；单批次上限 500，超限分页处理，
 * 单键读取失败跳过不中断。
 */
public interface TwinRealtimeMappingService {

    /**
     * 批量实时映射。
     *
     * @param request 设备编码列表或区域ID（二选一）
     * @return 实时映射聚合结果
     */
    RealtimeMappingVO mapping(RealtimeMappingRequest request);
}
