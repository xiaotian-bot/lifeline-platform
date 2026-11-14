package com.tianxin.lifeline.api.ai;

import com.tianxin.lifeline.api.ai.dto.AgentChatRequestDTO;
import com.tianxin.lifeline.api.ai.dto.AgentChatResultDTO;
import com.tianxin.lifeline.api.ai.dto.AgentJudgeRequestDTO;
import com.tianxin.lifeline.api.ai.dto.AgentJudgeResultDTO;
import com.tianxin.lifeline.api.ai.dto.AgentWorkflowResultDTO;
import com.tianxin.lifeline.api.ai.dto.AnomalyRequestDTO;
import com.tianxin.lifeline.api.ai.dto.AnomalyResultDTO;
import com.tianxin.lifeline.api.ai.dto.ForecastRequestDTO;
import com.tianxin.lifeline.api.ai.dto.ForecastResultDTO;
import com.tianxin.lifeline.api.ai.dto.PlanAgentRequestDTO;
import com.tianxin.lifeline.api.ai.dto.PlanAgentResultDTO;
import com.tianxin.lifeline.api.ai.dto.VisionAnalyzeRequestDTO;
import com.tianxin.lifeline.api.ai.dto.VisionAnalyzeResultDTO;
import com.tianxin.lifeline.common.core.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * AI能力网关服务（ai-gateway-service）Feign 客户端。
 *
 * <p>统一AI能力出入口，供预警等服务调用时序异常检测与趋势预报、
 * 智能预案生成与施工影像视觉分析能力。
 */
@FeignClient(name = "ai-gateway-service", path = "/api/v1/ai")
public interface AiGatewayFeignClient {

    /**
     * 时序异常检测。
     *
     * @param request 异常检测请求
     * @return 异常检测结果
     */
    @PostMapping("/anomaly-detect")
    Result<AnomalyResultDTO> detectAnomaly(@RequestBody AnomalyRequestDTO request);

    /**
     * 时序趋势预报。
     *
     * @param request 预报请求
     * @return 预报结果
     */
    @PostMapping("/forecast")
    Result<ForecastResultDTO> forecast(@RequestBody ForecastRequestDTO request);

    /**
     * 预警深度研判（AI 研判代理）。
     *
     * <p>对预警事件进行根因分析、等级复核、影响范围与趋势研判。
     *
     * @param request 研判请求
     * @return 研判结果
     */
    @PostMapping("/agent/judge")
    Result<AgentJudgeResultDTO> judgeWarning(@RequestBody AgentJudgeRequestDTO request);

    /**
     * 研判-预案一体化工作流。
     *
     * <p>入参同研判请求，一次调用完成「深度研判 + 预案生成」。
     *
     * @param request 研判请求（同 /agent/judge 入参）
     * @return judge + plan 合并结果
     */
    @PostMapping("/agent/workflow")
    Result<AgentWorkflowResultDTO> runWorkflow(@RequestBody AgentJudgeRequestDTO request);

    /**
     * 管网安全运维问答（AI 问答代理）。
     *
     * @param request 问答请求
     * @return 问答结果
     */
    @PostMapping("/agent/chat")
    Result<AgentChatResultDTO> chat(@RequestBody AgentChatRequestDTO request);

    /**
     * 智能预案生成（AI 预案代理）。
     *
     * <p>根据预警事件上下文生成处置预案建议（任务清单/资源清单/处置建议）。
     *
     * @param request 预案生成请求
     * @return 预案生成结果
     */
    @PostMapping("/agent/plan")
    Result<PlanAgentResultDTO> generatePlan(@RequestBody PlanAgentRequestDTO request);

    /**
     * 施工影像视觉分析。
     *
     * <p>对施工现场影像进行违规行为识别，返回违规结论与证据。
     *
     * @param request 视觉分析请求
     * @return 视觉分析结果
     */
    @PostMapping("/vision/analyze")
    Result<VisionAnalyzeResultDTO> analyzeVision(@RequestBody VisionAnalyzeRequestDTO request);
}
