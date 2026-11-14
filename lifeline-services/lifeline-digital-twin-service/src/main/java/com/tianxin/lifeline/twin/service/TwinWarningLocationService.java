package com.tianxin.lifeline.twin.service;

import com.tianxin.lifeline.twin.dto.WarningLocationResultVO;

/**
 * 预警定位服务。
 *
 * <p>按预警ID、管线活跃预警或区域活跃预警拉取预警列表，拼装设备/管线位置定位数据；
 * 预警携带设备时经 Redis 取实时值；结果缓存 Redis TTL 1 分钟。
 */
public interface TwinWarningLocationService {

    /**
     * 查询预警定位数据。
     *
     * @param warningId 预警事件ID（与 pipeId/areaId 三选一）
     * @param pipeId    管网ID，查该管线活跃预警（与 warningId/areaId 三选一）
     * @param areaId    区域ID，查该区域活跃预警（与 warningId/pipeId 三选一）
     * @return 预警定位聚合结果
     */
    WarningLocationResultVO locate(Long warningId, Long pipeId, Long areaId);
}
