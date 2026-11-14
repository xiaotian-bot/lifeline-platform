package com.tianxin.lifeline.warning.controller;

import com.tianxin.lifeline.common.core.page.PageQuery;
import com.tianxin.lifeline.common.core.page.PageResult;
import com.tianxin.lifeline.common.core.result.Result;
import com.tianxin.lifeline.warning.dto.WarningRuleRequest;
import com.tianxin.lifeline.warning.entity.WarningRule;
import com.tianxin.lifeline.warning.service.WarningRuleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 预警规则 REST 控制器。
 *
 * <p>统一路径前缀 /api/v1/warning-rules，提供规则分页查询与
 * 增删改（含启用/停用），Controller 只做参数校验与 Result 包装。
 */
@RestController
@RequestMapping("/api/v1/warning-rules")
public class WarningRuleController {

    /** 预警规则管理服务 */
    private final WarningRuleService warningRuleService;

    /**
     * 构造控制器。
     *
     * @param warningRuleService 预警规则管理服务
     */
    public WarningRuleController(WarningRuleService warningRuleService) {
        this.warningRuleService = warningRuleService;
    }

    /**
     * 分页查询预警规则。
     *
     * @param query 分页参数
     * @return 分页结果
     */
    @GetMapping
    public Result<PageResult<WarningRule>> page(@Valid PageQuery query) {
        return Result.success(warningRuleService.page(query));
    }

    /**
     * 创建预警规则。
     *
     * @param request 规则请求
     * @return 创建后的规则（含ID）
     */
    @PostMapping
    public Result<WarningRule> create(@Valid @RequestBody WarningRuleRequest request) {
        return Result.success(warningRuleService.create(request));
    }

    /**
     * 更新预警规则。
     *
     * @param id      规则ID
     * @param request 规则请求
     * @return 更新后的规则
     */
    @PutMapping("/{id}")
    public Result<WarningRule> update(@PathVariable("id") Long id,
                                      @Valid @RequestBody WarningRuleRequest request) {
        return Result.success(warningRuleService.update(id, request));
    }

    /**
     * 启用/停用预警规则。
     *
     * @param id     规则ID
     * @param status 目标状态：0 停用 / 1 启用
     * @return 更新后的规则
     */
    @PutMapping("/{id}/status")
    public Result<WarningRule> changeStatus(@PathVariable("id") Long id,
                                            @RequestParam("status") Integer status) {
        return Result.success(warningRuleService.changeStatus(id, status));
    }

    /**
     * 逻辑删除预警规则。
     *
     * @param id 规则ID
     * @return 空成功响应
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        warningRuleService.delete(id);
        return Result.success();
    }
}
