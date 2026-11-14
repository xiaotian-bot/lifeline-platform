package com.tianxin.lifeline.api.twin.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 模型数据聚合 DTO（服务间共享）。
 */
@Data
public class ModelDataDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 查询的管网类型 */
    private String pipeType;

    /** 查询的区域ID */
    private Long areaId;

    /** 管线数量 */
    private long pipeCount;

    /** 设备数量 */
    private long deviceCount;

    /** 井盖数量 */
    private long manholeCount;

    /** 管线总长度（米） */
    private double totalLengthMeters;

    /** GeoJSON 要素数量 */
    private long geoFeatureCount;

    /** GeoJSON 概要 */
    private List<Map<String, Object>> geoFeatureSummary = new ArrayList<>();

    /** 是否发生降级 */
    private boolean degraded;

    /** 降级的数据源说明 */
    private List<String> degradedSources = new ArrayList<>();
}
