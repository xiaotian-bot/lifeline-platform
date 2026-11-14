package com.tianxin.lifeline.asset.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianxin.lifeline.asset.dto.ManholePageRequest;
import com.tianxin.lifeline.asset.entity.Manhole;
import com.tianxin.lifeline.asset.mapper.ManholeMapper;
import com.tianxin.lifeline.asset.service.ManholeService;
import com.tianxin.lifeline.common.core.page.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 井室服务实现。
 *
 * <p>井室经纬度经 ST_X/ST_Y 提取。
 */
@Slf4j
@Service
public class ManholeServiceImpl implements ManholeService {

    /** 井室 Mapper */
    private final ManholeMapper manholeMapper;

    /**
     * 构造井室服务。
     *
     * @param manholeMapper 井室 Mapper
     */
    public ManholeServiceImpl(ManholeMapper manholeMapper) {
        this.manholeMapper = manholeMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult<Manhole> pageManholes(ManholePageRequest request) {
        Page<Manhole> page = new Page<>(request.getPage(), request.getSize());
        IPage<Manhole> result = manholeMapper.selectManholePage(
                page, request.getManholeType(), request.getStatus(), request.getKeyword());
        return PageResult.of(result.getRecords(), result.getTotal(), request.getPage(), request.getSize());
    }
}
