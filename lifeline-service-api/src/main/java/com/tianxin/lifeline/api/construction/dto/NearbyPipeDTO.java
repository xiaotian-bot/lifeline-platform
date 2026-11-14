package com.tianxin.lifeline.api.construction.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 周边管线影响范围查询结果 DTO（服务间共享）。
 */
@Data
public class NearbyPipeDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 管线ID */
    private Long id;

    /** 管线编码 */
    private String pipeCode;

    /** 管线名称 */
    private String pipeName;

    /** 管线类型：gas/water/drainage/heating/storm */
    private String pipeType;

    /** 管线状态 */
    private Integer status;

    /** 与施工点位的球面距离（米） */
    private Double distanceMeters;
}
