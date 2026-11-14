package com.tianxin.lifeline.construction.dto;

import com.tianxin.lifeline.common.core.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 施工项目分页查询请求。
 *
 * <p>支持项目状态与区域/关键字组合过滤，默认按创建时间降序。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProjectQueryRequest extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 项目状态：0 待开工 / 1 施工中 / 2 已完工 / 3 已停工 */
    private Integer status;

    /** 区域/关键字（匹配项目名称、施工地点、施工单位） */
    private String area;
}
