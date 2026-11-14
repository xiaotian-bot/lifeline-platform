package com.tianxin.lifeline.api.construction.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 施工项目创建/更新请求 DTO（服务间共享）。
 */
@Data
public class ProjectCreateRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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

    /** 施工点位经度（WGS84） */
    private Double longitude;

    /** 施工点位纬度（WGS84） */
    private Double latitude;
}
