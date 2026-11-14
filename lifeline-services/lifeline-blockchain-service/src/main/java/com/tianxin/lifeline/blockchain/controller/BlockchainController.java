package com.tianxin.lifeline.blockchain.controller;

import com.tianxin.lifeline.blockchain.dto.DidRegisterRequest;
import com.tianxin.lifeline.blockchain.dto.DidVO;
import com.tianxin.lifeline.blockchain.dto.EvidenceRequest;
import com.tianxin.lifeline.blockchain.dto.EvidenceTraceVO;
import com.tianxin.lifeline.blockchain.dto.EvidenceVO;
import com.tianxin.lifeline.blockchain.dto.EvidenceVerifyVO;
import com.tianxin.lifeline.blockchain.service.DidService;
import com.tianxin.lifeline.blockchain.service.EvidenceService;
import com.tianxin.lifeline.common.core.page.PageResult;
import com.tianxin.lifeline.common.core.result.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 区块链存证 REST 控制器。
 *
 * <p>统一路径前缀 /api/v1/blockchain（网关已配置路由），
 * Controller 只做参数校验与 Result 包装，业务逻辑下沉服务层。
 */
@RestController
@RequestMapping("/api/v1/blockchain")
public class BlockchainController {

    /** DID 业务服务 */
    private final DidService didService;

    /** 存证业务服务 */
    private final EvidenceService evidenceService;

    /**
     * 构造控制器。
     *
     * @param didService      DID 业务服务
     * @param evidenceService 存证业务服务
     */
    public BlockchainController(DidService didService, EvidenceService evidenceService) {
        this.didService = didService;
        this.evidenceService = evidenceService;
    }

    /**
     * 分页查询存证记录。
     *
     * @param page 页码
     * @param size 每页条数
     * @return 存证分页列表
     */
    @GetMapping("/evidence")
    public Result<PageResult<Map<String, Object>>> listEvidence(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return Result.success(evidenceService.pageEvidence(page, size));
    }

    /**
     * 查询存证详情。
     *
     * @param evidenceId 存证ID
     * @return 存证详情
     */
    @GetMapping("/evidence/{evidenceId}")
    public Result<Map<String, Object>> getEvidence(@PathVariable("evidenceId") Long evidenceId) {
        return Result.success(evidenceService.getEvidenceById(evidenceId));
    }

    /**
     * 按管道ID查询 DID。
     *
     * @param pipeId 管道ID
     * @return DID 信息
     */
    @GetMapping("/did/{pipeId}")
    public Result<DidVO> getDidByPipeId(@PathVariable("pipeId") Long pipeId) {
        DidVO did = didService.getByPipeId(pipeId);
        if (did == null) {
            return Result.success(null);
        }
        return Result.success(did);
    }

    /**
     * 注册管网 DID（幂等：同 pipeId 已注册返回既有 DID）。
     *
     * @param request 注册请求（pipeId / pipeCode）
     * @return DID 注册结果
     */
    @PostMapping("/did")
    public Result<DidVO> registerDid(@Valid @RequestBody DidRegisterRequest request) {
        return Result.success(didService.register(request));
    }

    /**
     * 存证上链：计算哈希落库并经账本适配器上链。
     *
     * @param request 存证请求（evidenceType / relatedId / relatedType / content）
     * @return 存证结果（evidenceId / evidenceHash / chainStatus / chainTime 等）
     */
    @PostMapping("/evidence")
    public Result<EvidenceVO> depositEvidence(@Valid @RequestBody EvidenceRequest request) {
        return Result.success(evidenceService.deposit(request));
    }

    /**
     * 按 DID 溯源：聚合该管道全环节存证时间线（时间倒序）。
     *
     * @param did DID 标识（did:lifeline:{pipeCode}:{随机后缀}）
     * @return 溯源时间线
     */
    @GetMapping("/trace/{did}")
    public Result<EvidenceTraceVO> trace(@PathVariable("did") String did) {
        return Result.success(evidenceService.trace(did));
    }

    /**
     * 防篡改校验：重算哈希与落库哈希比对。
     *
     * @param evidenceId 存证记录ID
     * @return 校验结果（tampered + 哈希值）
     */
    @GetMapping("/verify/{evidenceId}")
    public Result<EvidenceVerifyVO> verify(@PathVariable("evidenceId") Long evidenceId) {
        return Result.success(evidenceService.verify(evidenceId));
    }

    /**
     * 分页查询 DID 列表。
     *
     * @param page 页码（默认 1）
     * @param size 每页条数（默认 20）
     * @return DID 列表
     */
    @GetMapping("/dids")
    public Result<List<DidVO>> listDids(@RequestParam(value = "page", defaultValue = "1") int page,
                                        @RequestParam(value = "size", defaultValue = "20") int size) {
        return Result.success(didService.listDids(page, size));
    }
}
