package com.tianxin.lifeline.blockchain.service;

import com.tianxin.lifeline.blockchain.dto.DidRegisterRequest;
import com.tianxin.lifeline.blockchain.dto.DidVO;

import java.util.List;

/**
 * 管网 DID 数字身份业务服务。
 */
public interface DidService {

    /**
     * 注册管网 DID（幂等：同一 pipeId 已注册则返回既有 DID）。
     *
     * @param request 注册请求（pipeId / pipeCode）
     * @return DID 注册结果
     */
    DidVO register(DidRegisterRequest request);

    /**
     * 分页查询 DID 列表。
     *
     * @param page 页码（从 1 开始）
     * @param size 每页条数
     * @return DID 列表
     */
    List<DidVO> listDids(int page, int size);

    /**
     * 按管道ID查询 DID。
     *
     * @param pipeId 管道ID
     * @return DID 视图，不存在返回 null
     */
    DidVO getByPipeId(Long pipeId);
}
