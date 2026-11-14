package com.tianxin.lifeline.asset.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 新增/更新管网请求。
 */
@Data
public class PipeCreateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 管网编码 */
    @NotBlank(message = "管网编码不能为空")
    private String pipeCode;

    /** 管网名称 */
    private String pipeName;

    /** 管网类型：gas/water/drainage/heating/storm */
    @NotBlank(message = "管网类型不能为空")
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

    /** 状态：0 停用 / 1 运行，默认 1 */
    private Integer status = 1;

    /** 几何 WKT 文本（LINESTRING(...)） */
    private String geomWkt;

    /** 扩展属性（键值对，值为 [值, 单位] 或直接为字符串） */
    private List<Map<String, String>> attributes;
}
