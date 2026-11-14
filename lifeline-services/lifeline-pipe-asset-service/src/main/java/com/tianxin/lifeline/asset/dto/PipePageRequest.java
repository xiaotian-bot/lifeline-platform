package com.tianxin.lifeline.asset.dto;

import com.tianxin.lifeline.common.core.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 管网分页查询请求。
 *
 * <p>支持按类型、区域（名称关键字）、状态筛选。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PipePageRequest extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 管网类型：gas/water/drainage/heating/storm */
    private String pipeType;

    /** 区域/名称关键字（模糊匹配 pipe_name / pipe_code） */
    private String area;

    /** 状态：0 停用 / 1 运行 */
    private Integer status;
}
