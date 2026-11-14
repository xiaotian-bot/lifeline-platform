package com.tianxin.lifeline.api.construction.dto;

import com.tianxin.lifeline.common.core.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 施工项目分页查询请求 DTO（服务间共享）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProjectQueryRequestDTO extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 项目状态：0 待开工 / 1 施工中 / 2 已完工 / 3 已停工 */
    private Integer status;

    /** 区域/关键字 */
    private String area;
}
