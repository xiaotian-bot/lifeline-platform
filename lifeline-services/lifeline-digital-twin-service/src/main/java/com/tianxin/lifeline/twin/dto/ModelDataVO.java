package com.tianxin.lifeline.twin.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 模型数据聚合视图（GET /model-data）。
 *
 * <p>按 pipeType/areaId 聚合 pipe-asset 的管线/设备/井盖统计与 GeoJSON 概要。
 * 任一依赖失败仅降级对应分片（空集合 + degraded 标记），接口永不 5xx。
 */
@Data
public class ModelDataVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 查询的管网类型（可空表示全部类型） */
    private String pipeType;

    /** 查询的区域ID（可空表示全部区域） */
    private Long areaId;

    /** 管线数量 */
    private long pipeCount;

    /** 设备数量（井盖/阀门/泵站/传感器合计） */
    private long deviceCount;

    /** 井盖数量（deviceType=井盖 的设备） */
    private long manholeCount;

    /** 管线总长度（米） */
    private double totalLengthMeters;

    /** GeoJSON 要素数量（FeatureCollection.features 大小） */
    private long geoFeatureCount;

    /** GeoJSON 概要（仅前 N 个 Feature，控制响应体积） */
    private List<Map<String, Object>> geoFeatureSummary = new ArrayList<>();

    /** 是否发生降级（任一数据源失败即为 true） */
    private boolean degraded;

    /** 降级的数据源说明（如 pipe-list / device-list / geojson） */
    private List<String> degradedSources = new ArrayList<>();
}
