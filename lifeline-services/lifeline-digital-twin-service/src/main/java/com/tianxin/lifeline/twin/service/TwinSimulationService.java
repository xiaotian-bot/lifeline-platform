package com.tianxin.lifeline.twin.service;

import com.tianxin.lifeline.twin.dto.SimulationRequest;
import com.tianxin.lifeline.twin.dto.SimulationResultVO;
import com.tianxin.lifeline.twin.dto.SimulationSubmitVO;

/**
 * 仿真推演服务。
 *
 * <p>提交任务立即返回 taskId（异步线程池执行轻量规则推演，不阻塞请求），
 * 任务状态与结果由 Redis HASH 承载（TTL 30 分钟），经 taskId 轮询查询。
 */
public interface TwinSimulationService {

    /**
     * 提交仿真任务。
     *
     * @param request 场景类型与参数
     * @return 任务ID与初始状态
     */
    SimulationSubmitVO submit(SimulationRequest request);

    /**
     * 查询仿真任务状态与结果。
     *
     * @param taskId 任务ID
     * @return 任务状态与结构化结果
     */
    SimulationResultVO query(String taskId);
}
