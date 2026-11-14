package com.tianxin.lifeline.blockchain.service;

import com.tianxin.lifeline.blockchain.dto.EvidenceRequest;
import com.tianxin.lifeline.blockchain.dto.EvidenceRequestPayload;
import com.tianxin.lifeline.blockchain.dto.EvidenceTraceVO;
import com.tianxin.lifeline.blockchain.dto.EvidenceVO;
import com.tianxin.lifeline.blockchain.dto.EvidenceVerifyVO;
import com.tianxin.lifeline.common.core.page.PageResult;

import java.util.Map;

/**
 * 区块链存证业务服务。
 */
public interface EvidenceService {

    /**
     * REST 存证：计算哈希落库并上链。
     *
     * @param request 存证请求
     * @return 存证结果（含哈希/上链状态/模拟回执）
     */
    EvidenceVO deposit(EvidenceRequest request);

    /**
     * Kafka 事件存证：按生产方契约负载自动存证上链。
     *
     * @param payload 存证请求事件负载
     * @return 存证结果
     */
    EvidenceVO depositFromPayload(EvidenceRequestPayload payload);

    /**
     * 按 DID 溯源：聚合该管道全环节存证时间线（时间倒序）。
     *
     * @param did DID 标识
     * @return 溯源时间线视图
     */
    EvidenceTraceVO trace(String did);

    /**
     * 防篡改校验：重算哈希与落库哈希比对。
     *
     * @param evidenceId 存证记录ID
     * @return 校验结果（tampered + 哈希值）
     */
    EvidenceVerifyVO verify(Long evidenceId);

    /**
     * 分页查询存证记录。
     *
     * @param page 页码
     * @param size 每页条数
     * @return 分页结果
     */
    PageResult<Map<String, Object>> pageEvidence(int page, int size);

    /**
     * 按ID查询存证记录。
     *
     * @param evidenceId 存证ID
     * @return 存证记录
     */
    Map<String, Object> getEvidenceById(Long evidenceId);
}
