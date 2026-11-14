package com.tianxin.lifeline.asset.dto;

import com.tianxin.lifeline.common.core.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 井室分页查询请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ManholePageRequest extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 井室类型 */
    private String manholeType;

    /** 状态：0 停用 / 1 正常 */
    private Integer status;

    /** 井室编码关键字 */
    private String keyword;
}
