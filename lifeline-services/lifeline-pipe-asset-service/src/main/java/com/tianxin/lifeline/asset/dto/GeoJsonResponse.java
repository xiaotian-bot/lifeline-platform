package com.tianxin.lifeline.asset.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GeoJSON FeatureCollection 响应。
 *
 * <p>结构遵循 RFC 7946：{@code {"type":"FeatureCollection","features":[...]}}，
 * 每个 Feature 的 geometry 由 PostGIS {@code ST_AsGeoJSON} 输出。
 */
@Data
public class GeoJsonResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 固定值 FeatureCollection */
    private String type = "FeatureCollection";

    /** 要素列表，每项含 type/geometry/properties */
    private List<Map<String, Object>> features = new ArrayList<>();
}
