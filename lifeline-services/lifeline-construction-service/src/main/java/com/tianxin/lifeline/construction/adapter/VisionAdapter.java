package com.tianxin.lifeline.construction.adapter;

import com.tianxin.lifeline.api.ai.dto.VisionAnalyzeRequestDTO;
import com.tianxin.lifeline.api.ai.dto.VisionAnalyzeResultDTO;
import com.tianxin.lifeline.construction.constant.ViolationStatus;

/**
 * 施工影像视觉质检适配器接口。
 *
 * <p>「接口永不失败」契约：任何 AI 服务不可用、调用异常、返回无效
 * 的场景均由实现类内部降级为「未识别/人工复核」结论，绝不向调用方抛异常。
 */
public interface VisionAdapter {

    /**
     * 对施工影像发起视觉分析（失败降级，永不失败）。
     *
     * @param request 视觉分析请求（摄像头ID/项目ID/影像地址/影像类型）
     * @return 视觉分析结果，AI 不可用时返回降级结果（violationType = MANUAL_REVIEW）
     */
    VisionAnalyzeResultDTO analyze(VisionAnalyzeRequestDTO request);

    /**
     * 判断分析结果是否为降级结论（未识别，转人工复核）。
     *
     * @param result 视觉分析结果
     * @return true 表示降级结论
     */
    static boolean isDegraded(VisionAnalyzeResultDTO result) {
        return result != null && ViolationStatus.TYPE_MANUAL_REVIEW.equals(result.getViolationType());
    }
}
