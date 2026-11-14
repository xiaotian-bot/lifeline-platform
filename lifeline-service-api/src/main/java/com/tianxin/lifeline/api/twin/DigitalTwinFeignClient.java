package com.tianxin.lifeline.api.twin;

import com.tianxin.lifeline.api.twin.dto.ModelDataDTO;
import com.tianxin.lifeline.api.twin.dto.RealtimeMappingDTO;
import com.tianxin.lifeline.api.twin.dto.RealtimeMappingRequestDTO;
import com.tianxin.lifeline.api.twin.dto.SimulationRequestDTO;
import com.tianxin.lifeline.api.twin.dto.SimulationResultDTO;
import com.tianxin.lifeline.api.twin.dto.SimulationSubmitDTO;
import com.tianxin.lifeline.api.twin.dto.WarningLocationResultDTO;
import com.tianxin.lifeline.common.core.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 数字孪生服务（digital-twin-service）Feign 客户端。
 *
 * <p>供预警/预案等服务查询模型数据、实时映射、预警定位与仿真推演。
 */
@FeignClient(name = "digital-twin-service", path = "/api/v1/twin")
public interface DigitalTwinFeignClient {

    /**
     * 模型数据聚合。
     *
     * @param pipeType 管网类型（可空表示全部类型）
     * @param areaId   区域ID（可空表示全部区域）
     * @return 模型数据聚合视图
     */
    @GetMapping("/model-data")
    Result<ModelDataDTO> modelData(
            @RequestParam(value = "pipeType", required = false) String pipeType,
            @RequestParam(value = "areaId", required = false) Long areaId);

    /**
     * 实时映射。
     *
     * @param request 实时映射请求
     * @return 实时映射聚合结果
     */
    @PostMapping("/realtime-mapping")
    Result<RealtimeMappingDTO> realtimeMapping(@RequestBody RealtimeMappingRequestDTO request);

    /**
     * 预警定位。
     *
     * @param warningId 预警事件ID
     * @param pipeId    管网ID
     * @param areaId    区域ID
     * @return 预警定位聚合结果
     */
    @GetMapping("/warning-location")
    Result<WarningLocationResultDTO> warningLocation(
            @RequestParam(value = "warningId", required = false) Long warningId,
            @RequestParam(value = "pipeId", required = false) Long pipeId,
            @RequestParam(value = "areaId", required = false) Long areaId);

    /**
     * 提交仿真推演任务。
     *
     * @param request 场景类型与参数
     * @return 任务ID与初始状态
     */
    @PostMapping("/simulation")
    Result<SimulationSubmitDTO> submitSimulation(@RequestBody SimulationRequestDTO request);

    /**
     * 轮询仿真推演任务状态与结果。
     *
     * @param taskId 任务ID
     * @return 任务状态与结构化结果
     */
    @GetMapping("/simulation/{taskId}")
    Result<SimulationResultDTO> querySimulation(@PathVariable("taskId") String taskId);
}
