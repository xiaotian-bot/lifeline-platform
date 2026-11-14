package com.tianxin.lifeline.api.plan;

import com.tianxin.lifeline.api.plan.dto.PlanDetailDTO;
import com.tianxin.lifeline.api.plan.dto.PlanGenerateRequestDTO;
import com.tianxin.lifeline.api.plan.dto.PlanInstanceDTO;
import com.tianxin.lifeline.api.plan.dto.PlanTaskDTO;
import com.tianxin.lifeline.api.plan.dto.PlanTaskUpdateRequestDTO;
import com.tianxin.lifeline.api.plan.dto.PlanTemplateDTO;
import com.tianxin.lifeline.api.plan.dto.PlanTemplateSaveRequestDTO;
import com.tianxin.lifeline.common.core.page.PageResult;
import com.tianxin.lifeline.common.core.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 预案处置服务（plan-service）Feign 客户端。
 *
 * <p>供预警/数字孪生等上游服务查询预案模板、生成/执行预案实例。
 */
@FeignClient(name = "plan-service", path = "/api/v1/plans")
public interface PlanFeignClient {

    /**
     * 分页查询预案模板。
     *
     * @param keyword      模板名称关键字
     * @param templateType 模板类型
     * @param scenarioType 场景类型
     * @param status       状态
     * @param page         页码
     * @param size         每页条数
     * @return 分页结果
     */
    @GetMapping("/templates")
    Result<PageResult<PlanTemplateDTO>> pageTemplates(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "templateType", required = false) String templateType,
            @RequestParam(value = "scenarioType", required = false) String scenarioType,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size);

    /**
     * 查询预案模板详情。
     *
     * @param templateId 模板ID
     * @return 模板详情
     */
    @GetMapping("/templates/{templateId}")
    Result<PlanTemplateDTO> templateDetail(@PathVariable("templateId") Long templateId);

    /**
     * 启用/停用预案模板。
     *
     * @param templateId 模板ID
     * @param status     目标状态
     * @return 更新后的模板
     */
    @PutMapping("/templates/{templateId}/status")
    Result<PlanTemplateDTO> switchTemplateStatus(@PathVariable("templateId") Long templateId,
                                                 @RequestParam("status") Integer status);

    /**
     * 创建预案模板。
     *
     * @param request 模板保存请求
     * @return 创建后的模板
     */
    @PostMapping("/templates")
    Result<PlanTemplateDTO> createTemplate(@RequestBody PlanTemplateSaveRequestDTO request);

    /**
     * 更新预案模板。
     *
     * @param templateId 模板ID
     * @param request    模板保存请求
     * @return 更新后的模板
     */
    @PutMapping("/templates/{templateId}")
    Result<PlanTemplateDTO> updateTemplate(@PathVariable("templateId") Long templateId,
                                           @RequestBody PlanTemplateSaveRequestDTO request);

    /**
     * 生成处置预案。
     *
     * @param request 预案生成请求
     * @return 生成成功的预案实例
     */
    @PostMapping("/generate")
    Result<PlanInstanceDTO> generate(@RequestBody PlanGenerateRequestDTO request);

    /**
     * 查询预案实例详情。
     *
     * @param id 实例ID
     * @return 详情聚合视图
     */
    @GetMapping("/{id}")
    Result<PlanDetailDTO> detail(@PathVariable("id") Long id);

    /**
     * 启动执行预案。
     *
     * @param id            实例ID
     * @param dispatchOrder 是否联动派单
     * @return 流转后的实例
     */
    @PostMapping("/{id}/execute")
    Result<PlanInstanceDTO> execute(@PathVariable("id") Long id,
                                    @RequestParam(value = "dispatchOrder", defaultValue = "false")
                                    boolean dispatchOrder);

    /**
     * 查询实例下的任务清单。
     *
     * @param id 实例ID
     * @return 任务列表
     */
    @GetMapping("/{id}/tasks")
    Result<List<PlanTaskDTO>> tasks(@PathVariable("id") Long id);

    /**
     * 推进任务状态。
     *
     * @param id      任务ID
     * @param request 状态推进请求
     * @return 流转后的任务
     */
    @PutMapping("/tasks/{id}")
    Result<PlanTaskDTO> updateTask(@PathVariable("id") Long id,
                                   @RequestBody PlanTaskUpdateRequestDTO request);

    /**
     * 三级联动上报。
     *
     * @param id 实例ID
     * @return 流转后的实例
     */
    @PostMapping("/{id}/escalate")
    Result<PlanInstanceDTO> escalate(@PathVariable("id") Long id);

    /**
     * 取消预案。
     *
     * @param id 实例ID
     * @return 流转后的实例
     */
    @PostMapping("/{id}/cancel")
    Result<PlanInstanceDTO> cancel(@PathVariable("id") Long id);

    /**
     * 归档预案。
     *
     * @param id 实例ID
     * @return 流转后的实例
     */
    @PostMapping("/{id}/archive")
    Result<PlanInstanceDTO> archive(@PathVariable("id") Long id);
}
