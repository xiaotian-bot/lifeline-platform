package com.tianxin.lifeline.asset.controller;

import com.tianxin.lifeline.asset.dto.ManholePageRequest;
import com.tianxin.lifeline.asset.entity.Manhole;
import com.tianxin.lifeline.asset.service.ManholeService;
import com.tianxin.lifeline.common.core.page.PageResult;
import com.tianxin.lifeline.common.core.result.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 井室控制器。
 *
 * <p>提供井室台账分页查询（含经纬度）。
 * 控制器只做参数校验与 Result 包装，业务逻辑下沉 Service。
 */
@RestController
@RequestMapping("/api/v1/manholes")
public class ManholeController {

    /** 井室服务 */
    private final ManholeService manholeService;

    /**
     * 构造井室控制器。
     *
     * @param manholeService 井室服务
     */
    public ManholeController(ManholeService manholeService) {
        this.manholeService = manholeService;
    }

    /**
     * 分页查询井室（支持 manholeType/status/keyword 筛选，含经纬度，默认 20/页）。
     *
     * @param request 分页查询请求（query 参数绑定）
     * @return 井室分页结果
     */
    @GetMapping
    public Result<PageResult<Manhole>> pageManholes(@Valid ManholePageRequest request) {
        return Result.success(manholeService.pageManholes(request));
    }
}
