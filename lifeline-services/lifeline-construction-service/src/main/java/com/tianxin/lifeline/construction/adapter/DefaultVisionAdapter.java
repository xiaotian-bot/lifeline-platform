package com.tianxin.lifeline.construction.adapter;

import com.tianxin.lifeline.api.ai.AiGatewayFeignClient;
import com.tianxin.lifeline.api.ai.dto.VisionAnalyzeRequestDTO;
import com.tianxin.lifeline.api.ai.dto.VisionAnalyzeResultDTO;
import com.tianxin.lifeline.common.core.result.Result;
import com.tianxin.lifeline.construction.constant.ViolationStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 视觉质检适配器默认实现（失败降级）。
 *
 * <p>调用 {@link AiGatewayFeignClient#analyzeVision} 对施工影像做
 * 违规行为识别。
 *
 * <p>「失败即降级」契约：AI 服务不可用、调用异常、返回失败或结果
 * 数据无效时，统一返回「未识别/人工复核」降级结论
 * （violation=false、violationType=MANUAL_REVIEW、置信度0），
 * 保证视觉质检接口永不失败，由人工复核兜底。
 */
@Slf4j
@Component
public class DefaultVisionAdapter implements VisionAdapter {

    /** AI 能力网关 Feign 客户端 */
    private final AiGatewayFeignClient aiGatewayFeignClient;

    /**
     * 构造适配器。
     *
     * @param aiGatewayFeignClient AI 能力网关 Feign 客户端
     */
    public DefaultVisionAdapter(AiGatewayFeignClient aiGatewayFeignClient) {
        this.aiGatewayFeignClient = aiGatewayFeignClient;
    }

    /**
     * 调用 AI 视觉分析（失败降级返回「未识别/人工复核」结论，永不失败）。
     *
     * @param request 视觉分析请求
     * @return 视觉分析结果，AI 不可用时返回降级结果
     */
    @Override
    public VisionAnalyzeResultDTO analyze(VisionAnalyzeRequestDTO request) {
        if (request == null) {
            return fallbackResult("视觉分析请求为空");
        }
        try {
            Result<VisionAnalyzeResultDTO> result = aiGatewayFeignClient.analyzeVision(request);
            if (result == null || !result.isSuccess() || result.getData() == null) {
                log.warn("AI 视觉分析无有效结果, 降级人工复核: projectId={}, cameraId={}",
                        request.getProjectId(), request.getCameraId());
                return fallbackResult("AI 视觉分析服务无有效结果");
            }
            return result.getData();
        } catch (Exception e) {
            // AI 服务不可用/超时/熔断：降级人工复核，不阻塞质检主链路
            log.warn("AI 视觉分析调用失败, 降级人工复核: projectId={}, cameraId={}, cause={}",
                    request.getProjectId(), request.getCameraId(), e.getMessage());
            return fallbackResult("AI 视觉分析服务不可用（" + e.getMessage() + "）");
        }
    }

    /**
     * 构建「未识别/人工复核」降级结论。
     *
     * @param reason 降级原因（写入描述，便于复核人员知悉）
     * @return 降级分析结果
     */
    private VisionAnalyzeResultDTO fallbackResult(String reason) {
        VisionAnalyzeResultDTO fallback = new VisionAnalyzeResultDTO();
        fallback.setViolation(Boolean.FALSE);
        fallback.setViolationType(ViolationStatus.TYPE_MANUAL_REVIEW);
        fallback.setConfidence(0.0);
        fallback.setDescription("视觉未识别，已转人工复核。原因：" + reason);
        return fallback;
    }
}
