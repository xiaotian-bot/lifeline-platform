package com.tianxin.lifeline.plan.controller;

import com.tianxin.lifeline.common.core.page.PageResult;
import com.tianxin.lifeline.common.core.result.Result;
import com.tianxin.lifeline.plan.dto.PlanDetailVO;
import com.tianxin.lifeline.plan.dto.PlanGenerateRequest;
import com.tianxin.lifeline.plan.dto.PlanInstanceVO;
import com.tianxin.lifeline.plan.dto.PlanTaskUpdateRequest;
import com.tianxin.lifeline.plan.dto.PlanTaskVO;
import com.tianxin.lifeline.plan.dto.PlanTemplateQueryRequest;
import com.tianxin.lifeline.plan.dto.PlanTemplateSaveRequest;
import com.tianxin.lifeline.plan.entity.PlanTemplate;
import com.tianxin.lifeline.plan.service.PlanExecutionService;
import com.tianxin.lifeline.plan.service.PlanGenerateService;
import com.tianxin.lifeline.plan.service.PlanTemplateService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 预案处置 REST 控制器。
 *
 * <p>统一路径前缀 /api/v1/plans，与网关路由 /api/v1/plans/** → plan-service 对齐。
 * Controller 只做参数校验与 Result 包装，业务逻辑全部下沉服务层。
 */
@RestController
@RequestMapping("/api/v1/plans")
public class PlanController {

    /** 预案模板业务服务 */
    private final PlanTemplateService templateService;

    /** 预案生成业务服务 */
    private final PlanGenerateService generateService;

    /** 预案执行业务服务 */
    private final PlanExecutionService executionService;

    /**
     * 构造控制器（构造器注入）。
     *
     * @param templateService  预案模板业务服务
     * @param generateService  预案生成业务服务
     * @param executionService 预案执行业务服务
     */
    public PlanController(PlanTemplateService templateService,
                          PlanGenerateService generateService,
                          PlanExecutionService executionService) {
        this.templateService = templateService;
        this.generateService = generateService;
        this.executionService = executionService;
    }

    /**
     * 分页查询预案实例列表。
     *
     * @param page 页码（默认1）
     * @param size 每页条数（默认10）
     * @return 分页结果
     */
    @GetMapping("/instances")
    public Result<PageResult<PlanInstanceVO>> listInstances(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return Result.success(executionService.listInstances(page, size));
    }

    /**
     * 分页查询预案模板（支持关键字/类型/场景/状态过滤）。
     *
     * @param request 分页与过滤条件
     * @return 分页结果
     */
    @GetMapping("/templates")
    public Result<PageResult<PlanTemplate>> pageTemplates(@Valid PlanTemplateQueryRequest request) {
        return Result.success(templateService.page(request));
    }

    /**
     * 查询预案模板详情。
     *
     * @param templateId 模板ID
     * @return 模板详情
     */
    @GetMapping("/templates/{templateId}")
    public Result<PlanTemplate> templateDetail(@PathVariable("templateId") Long templateId) {
        return Result.success(templateService.detail(templateId));
    }

    /**
     * 启用/停用预案模板。
     *
     * @param templateId 模板ID
     * @param status     目标状态：1 启用 / 0 停用
     * @return 更新后的模板
     */
    @PutMapping("/templates/{templateId}/status")
    public Result<PlanTemplate> switchTemplateStatus(@PathVariable("templateId") Long templateId,
                                                     @RequestParam("status") Integer status) {
        return Result.success(templateService.switchStatus(templateId, status));
    }

    /**
     * 创建自定义预案模板（雪花 ID 手工填充，缺省启用）。
     *
     * @param request 模板保存请求
     * @return 创建后的模板
     */
    @PostMapping("/templates")
    public Result<PlanTemplate> createTemplate(@Valid @RequestBody PlanTemplateSaveRequest request) {
        return Result.success(templateService.create(request));
    }

    /**
     * 更新预案模板（按非空字段选择性覆盖）。
     *
     * @param templateId 模板ID
     * @param request    模板保存请求
     * @return 更新后的模板
     */
    @PutMapping("/templates/{templateId}")
    public Result<PlanTemplate> updateTemplate(@PathVariable("templateId") Long templateId,
                                               @Valid @RequestBody PlanTemplateSaveRequest request) {
        return Result.success(templateService.update(templateId, request));
    }

    /**
     * 生成处置预案（AI 优先，失败自动降级模板实例化，接口永不失败）。
     *
     * @param request 预案生成请求
     * @return 生成成功的预案实例
     */
    @PostMapping("/generate")
    public Result<PlanInstanceVO> generate(@Valid @RequestBody PlanGenerateRequest request) {
        return Result.success(generateService.generate(request));
    }

    /**
     * 查询预案实例详情（实例 + 任务 + 资源 + 日志聚合）。
     *
     * @param id 实例ID
     * @return 详情聚合视图
     */
    @GetMapping("/{id}")
    public Result<PlanDetailVO> detail(@PathVariable("id") Long id) {
        return Result.success(executionService.detail(id));
    }

    /**
     * 启动执行预案（状态机：0 草稿 → 1 执行中），
     * dispatchOrder=true 时联动工单服务派单（失败降级不阻断）。
     *
     * @param id            实例ID
     * @param dispatchOrder 是否联动派单，默认 false
     * @return 流转后的实例
     */
    @PostMapping("/{id}/execute")
    public Result<PlanInstanceVO> execute(@PathVariable("id") Long id,
                                          @RequestParam(value = "dispatchOrder", defaultValue = "false")
                                          boolean dispatchOrder) {
        return Result.success(executionService.execute(id, dispatchOrder));
    }

    /**
     * 查询实例下的任务清单。
     *
     * @param id 实例ID
     * @return 任务列表
     */
    @GetMapping("/{id}/tasks")
    public Result<List<PlanTaskVO>> tasks(@PathVariable("id") Long id) {
        return Result.success(executionService.listTasks(id));
    }

    /**
     * 推进任务状态（状态机校验，全部任务完成后实例自动核销）。
     *
     * @param id      任务ID
     * @param request 状态推进请求
     * @return 流转后的任务
     */
    @PutMapping("/tasks/{id}")
    public Result<PlanTaskVO> updateTask(@PathVariable("id") Long id,
                                         @Valid @RequestBody PlanTaskUpdateRequest request) {
        return Result.success(executionService.updateTask(id, request));
    }

    /**
     * 三级联动上报（仅执行中实例可上报，级别逐级 +1 至上限 2，
     * 政府侧外部系统对接仅留日志/事件位不做真实集成）。
     *
     * @param id 实例ID
     * @return 流转后的实例
     */
    @PostMapping("/{id}/escalate")
    public Result<PlanInstanceVO> escalate(@PathVariable("id") Long id) {
        return Result.success(executionService.escalate(id));
    }

    /**
     * 取消预案（草稿/执行中可取消 → 4 已取消）。
     *
     * @param id 实例ID
     * @return 流转后的实例
     */
    @PostMapping("/{id}/cancel")
    public Result<PlanInstanceVO> cancel(@PathVariable("id") Long id) {
        return Result.success(executionService.cancel(id));
    }

    /**
     * 归档预案（已完成可归档 → 3 已归档）。
     *
     * @param id 实例ID
     * @return 流转后的实例
     */
    @PostMapping("/{id}/archive")
    public Result<PlanInstanceVO> archive(@PathVariable("id") Long id) {
        return Result.success(executionService.archive(id));
    }
}
