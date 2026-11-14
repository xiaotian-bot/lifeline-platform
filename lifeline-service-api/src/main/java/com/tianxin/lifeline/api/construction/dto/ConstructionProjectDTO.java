package com.tianxin.lifeline.api.construction.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 施工项目 DTO（服务间共享）。
 *
 * <p>镜像 construction-service 的 ProjectVO，供跨服务调用。
 */
@Data
public class ConstructionProjectDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 项目ID */
    private Long id;

    /** 项目名称 */
    private String projectName;

    /** 施工地点 */
    private String location;

    /** 施工单位 */
    private String contractor;

    /** 监理负责人 */
    private String supervisor;

    /** 计划开工日期 */
    private LocalDate startDate;

    /** 计划竣工日期 */
    private LocalDate endDate;

    /** 项目状态：0 待开工 / 1 施工中 / 2 已完工 / 3 已停工 */
    private Integer status;

    /** 项目状态中文文本 */
    private String statusText;

    /** 经度 */
    private Double longitude;

    /** 纬度 */
    private Double latitude;

    /** 几何 WKT 文本 */
    private String geomWkt;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
