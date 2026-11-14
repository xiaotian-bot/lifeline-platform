package com.tianxin.lifeline.construction.service;

import com.tianxin.lifeline.construction.dto.ProcessCreateRequest;
import com.tianxin.lifeline.construction.dto.ProcessInspectRequest;
import com.tianxin.lifeline.construction.dto.VisionInspectRequest;
import com.tianxin.lifeline.construction.dto.VisionInspectResultVO;
import com.tianxin.lifeline.construction.entity.ConstructionProcess;

import java.util.List;

/**
 * 施工工序业务服务。
 *
 * <p>工序创建/清单、人工质检记录提交、AI 视觉质检
 * （失败降级人工复核，命中违规落违规记录并触发存证）。
 */
public interface ConstructionProcessService {

    /**
     * 查询项目下的工序清单（创建时间升序）。
     *
     * @param projectId 项目ID
     * @return 工序列表
     */
    List<ConstructionProcess> listByProject(Long projectId);

    /**
     * 创建工序（雪花 ID 手工填充，初始质检状态待质检）。
     *
     * @param projectId 项目ID
     * @param request   工序创建请求
     * @return 工序实体
     */
    ConstructionProcess create(Long projectId, ProcessCreateRequest request);

    /**
     * 提交人工质检记录（回写质检人/时间/得分/质检状态）。
     *
     * @param processId 工序ID
     * @param request   质检提交请求
     * @return 回写后的工序实体
     */
    ConstructionProcess inspect(Long processId, ProcessInspectRequest request);

    /**
     * AI 视觉质检：调视觉分析适配器（永不失败），分析结果回写
     * 工序质检记录；命中违规落违规记录并投递存证事件；
     * AI 降级时工序保持质检中并标记人工复核。
     *
     * @param processId 工序ID
     * @param request   视觉质检请求
     * @return 视觉质检结果视图
     */
    VisionInspectResultVO visionInspect(Long processId, VisionInspectRequest request);
}
