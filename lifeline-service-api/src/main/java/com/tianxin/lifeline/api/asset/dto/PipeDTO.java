package com.tianxin.lifeline.api.asset.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管网档案 DTO（服务间共享）。
 */
@Data
public class PipeDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 管网ID（JSON 序列化为 id，与前端 / PipeDetailResponse 对齐） */
    @JsonProperty("id")
    private Long pipeId;

    /** 管网编码 */
    private String pipeCode;

    /** 管网名称 */
    private String pipeName;

    /** 管网类型：WATER/HEAT/GAS/SEWAGE/HAZARD */
    private String pipeType;

    /** 管径（mm） */
    private Double diameter;

    /** 管材 */
    private String material;

    /** 管道长度（米） */
    private Double length;

    /** 埋深（米） */
    private Double buriedDepth;

    /** 所属区域ID */
    private Long areaId;

    /** 管线级别：主干管 / 支干线 / 支线 */
    private String pipeLevel;

    /** 投运时间 */
    private LocalDateTime commissionDate;

    /** 资产状态：入库/安装/运行/报废 */
    private String status;

    /** WKT 线串坐标（LINESTRING(...)） */
    private String geometryWkt;
}
