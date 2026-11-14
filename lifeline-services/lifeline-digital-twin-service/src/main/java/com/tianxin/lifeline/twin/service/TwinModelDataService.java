package com.tianxin.lifeline.twin.service;

import com.tianxin.lifeline.twin.dto.ModelDataVO;

/**
 * 模型数据聚合服务。
 *
 * <p>按 pipeType/areaId 并行聚合 pipe-asset 的管线/设备/井盖统计与 GeoJSON 概要，
 * Caffeine L1 缓存 30 秒；任一依赖失败降级为空骨架并标记 degraded，永不 5xx。
 */
public interface TwinModelDataService {

    /**
     * 聚合模型数据。
     *
     * @param pipeType 管网类型（可空表示全部类型）
     * @param areaId   区域ID（可空表示全部区域）
     * @return 模型数据聚合视图
     */
    ModelDataVO getModelData(String pipeType, Long areaId);
}
