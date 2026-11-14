package com.tianxin.lifeline.asset.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tianxin.lifeline.common.data.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDate;

/**
 * 管网设备实体（对应表 pipe_device，传感器/监测点）。
 *
 * <p>geom 字段为 PostGIS POINT(EPSG:4326)，在 XML 中经 ST_* 函数读写，
 * 以 {@link #geomWkt} 承载 WKT、{@link #longitude}/{@link #latitude} 承载坐标。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pipe_device")
public class PipeDevice extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 设备编码（DEV-{管网类型}-{三位序号}，如 DEV-GAS-001） */
    private String deviceCode;

    /** 设备名称 */
    private String deviceName;

    /** 设备类型：pressure/temp/flow/vibration/corrosion/leakage/video */
    private String deviceType;

    /** 所属管网ID */
    private Long pipeId;

    /** 状态：0 离线 / 1 在线 */
    private Integer status;

    /** 安装日期 */
    private LocalDate installDate;

    /** 几何 WKT 文本（POINT(...)），非数据库映射字段 */
    @TableField(exist = false)
    private String geomWkt;

    /** 经度，非数据库映射字段 */
    @TableField(exist = false)
    private Double longitude;

    /** 纬度，非数据库映射字段 */
    @TableField(exist = false)
    private Double latitude;

    /** 所属管网类型（JOIN pipe_main 回填），非数据库映射字段 */
    @TableField(exist = false)
    private String pipeType;
}
