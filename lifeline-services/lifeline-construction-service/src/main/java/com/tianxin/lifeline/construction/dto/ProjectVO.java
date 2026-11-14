package com.tianxin.lifeline.construction.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 施工项目视图对象（分页列表与详情共用）。
 *
 * <p>相对实体补充状态中文文本与经纬度坐标分解结果。
 */
@Data
public class ProjectVO implements Serializable {

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

    /** 经度（详情时回填） */
    private Double longitude;

    /** 纬度（详情时回填） */
    private Double latitude;

    /** 几何 WKT 文本（详情时回填） */
    private String geomWkt;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
