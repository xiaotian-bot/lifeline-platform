package com.tianxin.lifeline.construction.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tianxin.lifeline.common.data.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDate;

/**
 * 施工项目实体（对应表 construction_project）。
 *
 * <p>geom 字段为 PostGIS POINT(EPSG:4326)，不参与 MyBatis-Plus 自动 CRUD，
 * 统一在 XML 中通过 {@code ST_AsText}/{@code ST_GeomFromText}/{@code ST_X/ST_Y}
 * 读写（复用 pipe-asset 空间模式），并以 {@link #geomWkt} 承载 WKT 文本、
 * {@link #longitude}/{@link #latitude} 承载坐标分解结果。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("construction_project")
public class ConstructionProject extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 项目名称 */
    private String projectName;

    /** 施工地点（行政区位描述） */
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

    /** 施工点位几何 WKT 文本（POINT(经度 纬度)），非数据库映射字段 */
    @TableField(exist = false)
    private String geomWkt;

    /** 经度（ST_X 分解结果），非数据库映射字段 */
    @TableField(exist = false)
    private Double longitude;

    /** 纬度（ST_Y 分解结果），非数据库映射字段 */
    @TableField(exist = false)
    private Double latitude;
}
