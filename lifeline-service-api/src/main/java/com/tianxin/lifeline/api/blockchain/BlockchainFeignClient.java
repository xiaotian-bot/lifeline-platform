package com.tianxin.lifeline.api.blockchain;

import com.tianxin.lifeline.api.blockchain.dto.DidDTO;
import com.tianxin.lifeline.api.blockchain.dto.DidRegisterRequestDTO;
import com.tianxin.lifeline.api.blockchain.dto.EvidenceDTO;
import com.tianxin.lifeline.api.blockchain.dto.EvidenceRequestDTO;
import com.tianxin.lifeline.api.blockchain.dto.EvidenceTraceDTO;
import com.tianxin.lifeline.api.blockchain.dto.EvidenceVerifyDTO;
import com.tianxin.lifeline.common.core.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 区块链存证服务（blockchain-service）Feign 客户端。
 *
 * <p>供施工/预警/预案等服务触发 DID 注册、存证上链与溯源校验。
 */
@FeignClient(name = "blockchain-service", path = "/api/v1/blockchain")
public interface BlockchainFeignClient {

    /**
     * 注册管网 DID（幂等：同 pipeId 已注册返回既有 DID）。
     *
     * @param request 注册请求
     * @return DID 注册结果
     */
    @PostMapping("/did")
    Result<DidDTO> registerDid(@RequestBody DidRegisterRequestDTO request);

    /**
     * 存证上链。
     *
     * @param request 存证请求
     * @return 存证结果
     */
    @PostMapping("/evidence")
    Result<EvidenceDTO> depositEvidence(@RequestBody EvidenceRequestDTO request);

    /**
     * 按 DID 溯源：聚合该管道全环节存证时间线。
     *
     * @param did DID 标识
     * @return 溯源时间线
     */
    @GetMapping("/trace/{did}")
    Result<EvidenceTraceDTO> trace(@PathVariable("did") String did);

    /**
     * 防篡改校验：重算哈希与落库哈希比对。
     *
     * @param evidenceId 存证记录ID
     * @return 校验结果
     */
    @GetMapping("/verify/{evidenceId}")
    Result<EvidenceVerifyDTO> verify(@PathVariable("evidenceId") Long evidenceId);
}
