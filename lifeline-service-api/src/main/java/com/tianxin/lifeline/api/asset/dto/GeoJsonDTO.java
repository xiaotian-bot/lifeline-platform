package com.tianxin.lifeline.api.asset.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GeoJSON FeatureCollection DTO（服务间共享）。
 *
 * <p>结构遵循 RFC 7946：{@code {"type":"FeatureCollection","features":[...]}}，
 * 与 pipe-asset-service 的 GeoJsonResponse 字段一一对应。
 */
@Data
public class GeoJsonDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 固定值 FeatureCollection */
    private String type = "FeatureCollection";

    /** 要素列表，每项含 type/geometry/properties */
    private List<Map<String, Object>> features = new ArrayList<>();
}
