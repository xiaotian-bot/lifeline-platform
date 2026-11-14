package com.tianxin.lifeline.blockchain.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.tianxin.lifeline.blockchain.dto.DidRegisterRequest;
import com.tianxin.lifeline.blockchain.dto.DidVO;
import com.tianxin.lifeline.blockchain.entity.BlockchainDid;
import com.tianxin.lifeline.blockchain.mapper.BlockchainDidMapper;
import com.tianxin.lifeline.blockchain.service.DidService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 管网 DID 数字身份业务服务实现。
 *
 * <p>DID 格式：{@code did:lifeline:{pipeCode}:{8位随机后缀}}，
 * 同一 pipeId 幂等：已注册直接返回既有记录（reused=true）。
 */
@Slf4j
@Service
public class DidServiceImpl implements DidService {

    /** DID 前缀（平台命名空间） */
    private static final String DID_PREFIX = "did:lifeline:";

    /** DID 随机后缀长度 */
    private static final int DID_SUFFIX_LENGTH = 8;

    /** DID Mapper */
    private final BlockchainDidMapper didMapper;

    /**
     * 构造服务。
     *
     * @param didMapper DID Mapper
     */
    public DidServiceImpl(BlockchainDidMapper didMapper) {
        this.didMapper = didMapper;
    }

    /**
     * 注册管网 DID（幂等）。
     *
     * @param request 注册请求
     * @return DID 注册结果
     */
    @Override
    public DidVO register(DidRegisterRequest request) {
        // 幂等：同 pipeId 已注册则直接返回既有 DID
        BlockchainDid existing = didMapper.selectByPipeId(request.getPipeId());
        if (existing != null) {
            log.info("DID 已注册, 命中幂等返回: pipeId={}, did={}", request.getPipeId(), existing.getDid());
            return toVO(existing, true);
        }

        BlockchainDid entity = new BlockchainDid();
        entity.setPipeId(request.getPipeId());
        entity.setPipeCode(request.getPipeCode());
        entity.setDid(DID_PREFIX + request.getPipeCode() + ":" + RandomUtil.randomString(DID_SUFFIX_LENGTH));
        didMapper.insert(entity);
        log.info("DID 注册成功: pipeId={}, did={}", entity.getPipeId(), entity.getDid());
        return toVO(entity, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DidVO> listDids(int page, int size) {
        int offset = (page - 1) * size;
        List<BlockchainDid> records = didMapper.selectList(null);
        return records.stream()
                .skip(offset)
                .limit(size)
                .map(e -> toVO(e, false))
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DidVO getByPipeId(Long pipeId) {
        BlockchainDid existing = didMapper.selectByPipeId(pipeId);
        if (existing == null) {
            return null;
        }
        return toVO(existing, false);
    }

    /**
     * 实体转视图。
     *
     * @param entity DID 实体
     * @param reused 是否命中幂等
     * @return DID 视图
     */
    private DidVO toVO(BlockchainDid entity, boolean reused) {
        DidVO vo = new DidVO();
        vo.setId(entity.getId());
        vo.setDid(entity.getDid());
        vo.setPipeId(entity.getPipeId());
        vo.setPipeCode(entity.getPipeCode());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setReused(reused);
        return vo;
    }
}
