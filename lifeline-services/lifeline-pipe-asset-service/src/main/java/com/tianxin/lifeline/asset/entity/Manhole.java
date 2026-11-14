package com.tianxin.lifeline.asset.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tianxin.lifeline.common.data.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 井室实体（对应表 manhole，窨井/阀门井）。
 *
 * <p>geom 字段为 PostGIS POINT(EPSG:4326)，在 XML 中经 ST_* 函数读写。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("manhole")
public class Manhole extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 井室编码 */
    private String manholeCode;

    /** 井室类型 */
    private String manholeType;

    /** 井径（mm） */
    private BigDecimal diameter;

    /** 井深（米） */
    private BigDecimal depth;

    /** 状态：0 停用 / 1 正常 */
    private Integer status;

    /** 几何 WKT 文本（POINT(...)），非数据库映射字段 */
    @TableField(exist = false)
    private String geomWkt;

    /** 经度，非数据库映射字段 */
    @TableField(exist = false)
    private Double longitude;

    /** 纬度，非数据库映射字段 */
    @TableField(exist = false)
    private Double latitude;
}
