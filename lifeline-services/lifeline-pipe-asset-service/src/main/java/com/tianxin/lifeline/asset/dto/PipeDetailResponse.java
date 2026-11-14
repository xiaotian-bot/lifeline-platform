package com.tianxin.lifeline.asset.dto;

import com.tianxin.lifeline.asset.entity.PipeAttribute;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管网详情响应（含扩展属性）。
 */
@Data
public class PipeDetailResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 管网ID */
    private Long id;

    /** 管网编码 */
    private String pipeCode;

    /** 管网名称 */
    private String pipeName;

    /** 管网类型 */
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

    /** 几何 WKT 文本 */
    private String geomWkt;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 扩展属性列表 */
    private List<PipeAttribute> attributes;
}
