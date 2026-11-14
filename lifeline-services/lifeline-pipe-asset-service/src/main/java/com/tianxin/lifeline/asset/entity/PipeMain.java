package com.tianxin.lifeline.asset.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tianxin.lifeline.common.data.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 管网主线实体（对应表 pipe_main）。
 *
 * <p>geom 字段为 PostGIS LINESTRING(EPSG:4326)，不参与 MyBatis-Plus 自动 CRUD，
 * 统一在 XML 中通过 {@code ST_AsText}/{@code ST_GeomFromText} 读写，
 * 并以 {@link #geomWkt} 承载 WKT 文本。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pipe_main")
public class PipeMain extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 管网编码（如 PM-GAS-001） */
    private String pipeCode;

    /** 管网名称 */
    private String pipeName;

    /** 管网类型：gas/water/drainage/heating/storm */
    private String pipeType;

    /** 管径（mm） */
    private BigDecimal diameter;

    /** 管材 */
    private String material;

    /** 管道长度（米） */
    private BigDecimal length;

    /** 埋深（米） */
    private BigDecimal depth;

    /** 投运日期 */
    private LocalDate installDate;

    /** 状态：0 停用 / 1 运行 */
    private Integer status;

    /** 几何 WKT 文本（LINESTRING(...)），非数据库映射字段 */
    @TableField(exist = false)
    private String geomWkt;

    /** 几何 GeoJSON 文本，非数据库映射字段 */
    @TableField(exist = false)
    private String geomGeoJson;
}
