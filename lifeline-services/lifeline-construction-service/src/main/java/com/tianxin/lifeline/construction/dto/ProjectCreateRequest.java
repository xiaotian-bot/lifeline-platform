package com.tianxin.lifeline.construction.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 施工项目创建/更新请求。
 *
 * <p>施工点位以经纬度传入，服务端拼装 {@code POINT(经度 纬度)} WKT
 * 经 ST_GeomFromText 写入 PostGIS（EPSG:4326）。
 */
@Data
public class ProjectCreateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 项目名称 */
    @NotBlank(message = "项目名称不能为空")
    @Size(max = 128, message = "项目名称长度不能超过128")
    private String projectName;

    /** 施工地点（行政区位描述） */
    @Size(max = 255, message = "施工地点长度不能超过255")
    private String location;

    /** 施工单位 */
    @Size(max = 128, message = "施工单位长度不能超过128")
    private String contractor;

    /** 监理负责人 */
    @Size(max = 64, message = "监理负责人长度不能超过64")
    private String supervisor;

    /** 计划开工日期 */
    private LocalDate startDate;

    /** 计划竣工日期 */
    private LocalDate endDate;

    /** 施工点位经度（WGS84） */
    @DecimalMin(value = "-180.0", message = "经度不能小于-180")
    @DecimalMax(value = "180.0", message = "经度不能大于180")
    private Double longitude;

    /** 施工点位纬度（WGS84） */
    @DecimalMin(value = "-90.0", message = "纬度不能小于-90")
    @DecimalMax(value = "90.0", message = "纬度不能大于90")
    private Double latitude;
}
