package com.tianxin.lifeline.asset.service;

import com.tianxin.lifeline.asset.dto.ManholePageRequest;
import com.tianxin.lifeline.asset.entity.Manhole;
import com.tianxin.lifeline.common.core.page.PageResult;

/**
 * 井室服务。
 *
 * <p>职责：井室台账分页查询（含经纬度）。
 */
public interface ManholeService {

    /**
     * 分页查询井室（支持类型/状态/编码关键字筛选，含经纬度）。
     *
     * @param request 分页查询请求
     * @return 井室分页结果
     */
    PageResult<Manhole> pageManholes(ManholePageRequest request);
}
