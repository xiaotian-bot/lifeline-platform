package com.tianxin.lifeline.api.warning;

import com.tianxin.lifeline.api.warning.dto.WarningEventDTO;
import com.tianxin.lifeline.common.core.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 预警预报服务（warning-service）Feign 客户端。
 *
 * <p>供其他微服务查询/创建预警事件。
 */
@FeignClient(name = "warning-service", path = "/api/v1/warnings")
public interface WarningFeignClient {

    /**
     * 获取预警事件详情。
     *
     * @param warningId 预警事件ID
     * @return 预警事件
     */
    @GetMapping("/{warningId}")
    Result<WarningEventDTO> getWarningById(@PathVariable("warningId") Long warningId);

    /**
     * 查询指定管网的活跃预警列表。
     *
     * @param pipeId 管网ID
     * @return 预警事件列表
     */
    @GetMapping("/active")
    Result<List<WarningEventDTO>> listActiveWarnings(@RequestParam("pipeId") Long pipeId);

    /**
     * 查询指定区域的活跃预警列表（按 warning_event.area_id 过滤）。
     *
     * @param areaId 区域ID
     * @return 预警事件列表
     */
    @GetMapping("/active/area")
    Result<List<WarningEventDTO>> listActiveWarningsByArea(@RequestParam("areaId") Long areaId);

    /**
     * 创建预警事件（由规则引擎/AI模型等上游触发）。
     *
     * @param warningEvent 预警事件
     * @return 创建后的预警事件（含ID）
     */
    @PostMapping
    Result<WarningEventDTO> createWarning(@RequestBody WarningEventDTO warningEvent);
}
