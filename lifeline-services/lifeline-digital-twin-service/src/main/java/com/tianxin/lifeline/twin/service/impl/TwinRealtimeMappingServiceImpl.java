package com.tianxin.lifeline.twin.service.impl;

import com.tianxin.lifeline.api.asset.PipeAssetFeignClient;
import com.tianxin.lifeline.api.asset.dto.DeviceDTO;
import com.tianxin.lifeline.api.asset.dto.PipeDTO;
import com.tianxin.lifeline.common.core.exception.BizException;
import com.tianxin.lifeline.common.core.result.ErrorCode;
import com.tianxin.lifeline.twin.config.TwinProperties;
import com.tianxin.lifeline.twin.dto.DeviceRealtimeVO;
import com.tianxin.lifeline.twin.dto.RealtimeMappingRequest;
import com.tianxin.lifeline.twin.dto.RealtimeMappingVO;
import com.tianxin.lifeline.twin.service.RealtimeCacheReader;
import com.tianxin.lifeline.twin.service.TwinFeignSupport;
import com.tianxin.lifeline.twin.service.TwinRealtimeMappingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * 实时映射服务实现。
 *
 * <p>设备解析：deviceIds 直传优先；areaId 模式经 pipe-asset 五类管线并行拉取、
 * 区域过滤后展开设备清单（管线数截断至 {@value #MAX_AREA_PIPES}）。
 * 实时读取：按 {@code lifeline.twin.realtime-batch-size}（默认 500）分页，
 * 每页 Redis Pipeline 攒批读取；单键未命中跳过，整批异常登记降级不中断。
 */
@Slf4j
@Service
public class TwinRealtimeMappingServiceImpl implements TwinRealtimeMappingService {

    /** 全量管网类型编码（areaId 模式逐一并行拉取） */
    private static final List<String> ALL_PIPE_TYPES = List.of("WATER", "HEAT", "GAS", "SEWAGE", "HAZARD");

    /** areaId 模式参与设备解析的管线数量上限（保证接口时延） */
    private static final int MAX_AREA_PIPES = 50;

    /** 单次请求设备总数硬上限（与请求体 @Size 校验一致） */
    private static final int MAX_TOTAL_DEVICES = 5000;

    /** 降级来源标识：区域设备解析 */
    private static final String SOURCE_AREA_DEVICES = "area-device-resolve";

    /** 降级来源标识：Redis 实时缓存 */
    private static final String SOURCE_REDIS_RT = "redis-realtime";

    /** 管网资产 Feign 客户端 */
    private final PipeAssetFeignClient pipeAssetFeignClient;

    /** Feign 降级支撑 */
    private final TwinFeignSupport feignSupport;

    /** 实时缓存读取器 */
    private final RealtimeCacheReader realtimeCacheReader;

    /** 跨服务并行聚合线程池 */
    private final Executor feignExecutor;

    /** 业务参数 */
    private final TwinProperties properties;

    /**
     * 构造服务。
     *
     * @param pipeAssetFeignClient 管网资产 Feign 客户端
     * @param feignSupport         Feign 降级支撑
     * @param realtimeCacheReader  实时缓存读取器
     * @param feignExecutor        聚合线程池
     * @param properties           业务参数
     */
    public TwinRealtimeMappingServiceImpl(PipeAssetFeignClient pipeAssetFeignClient,
                                          TwinFeignSupport feignSupport,
                                          RealtimeCacheReader realtimeCacheReader,
                                          @Qualifier("twinFeignExecutor") Executor feignExecutor,
                                          TwinProperties properties) {
        this.pipeAssetFeignClient = pipeAssetFeignClient;
        this.feignSupport = feignSupport;
        this.realtimeCacheReader = realtimeCacheReader;
        this.feignExecutor = feignExecutor;
        this.properties = properties;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RealtimeMappingVO mapping(RealtimeMappingRequest request) {
        boolean hasDeviceIds = request.getDeviceIds() != null && !request.getDeviceIds().isEmpty();
        if (!hasDeviceIds && request.getAreaId() == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "deviceIds 与 areaId 至少提供其一");
        }

        RealtimeMappingVO vo = new RealtimeMappingVO();
        List<String> deviceIds;
        if (hasDeviceIds) {
            deviceIds = normalize(request.getDeviceIds());
        } else {
            deviceIds = resolveAreaDevices(request.getAreaId(), vo);
        }
        vo.setTotal(deviceIds.size());
        if (deviceIds.isEmpty()) {
            vo.setDegraded(!vo.getDegradedSources().isEmpty());
            return vo;
        }

        // 分页 Pipeline 批量读取（单批上限 realtime-batch-size）
        int batchSize = Math.max(1, properties.getRealtimeBatchSize());
        int pages = (deviceIds.size() + batchSize - 1) / batchSize;
        vo.setPages(pages);
        for (int from = 0; from < deviceIds.size(); from += batchSize) {
            List<String> page = deviceIds.subList(from, Math.min(from + batchSize, deviceIds.size()));
            readPage(vo, page);
        }
        vo.setDegraded(!vo.getDegradedSources().isEmpty());
        return vo;
    }

    /**
     * 入参设备编码清洗：去空白、去空值、去重、截断硬上限。
     *
     * @param raw 原始设备编码列表
     * @return 规范化后的设备编码列表
     */
    private List<String> normalize(List<String> raw) {
        Set<String> set = new LinkedHashSet<>();
        for (String id : raw) {
            if (id != null && !id.isBlank()) {
                set.add(id.trim());
            }
        }
        return set.stream().limit(MAX_TOTAL_DEVICES).toList();
    }

    /**
     * areaId 模式解析区域内设备编码：五类管线并行拉取 → 区域过滤 →
     * 截断后并行展开设备清单 → 收集设备编码。
     *
     * @param areaId 区域ID
     * @param vo     结果视图（登记降级来源）
     * @return 设备编码列表（解析失败降级为空列表）
     */
    private List<String> resolveAreaDevices(Long areaId, RealtimeMappingVO vo) {
        // 多线程并发登记降级来源，使用同步列表（与 model-data/warning-location 一致）
        List<String> degradedSources = Collections.synchronizedList(vo.getDegradedSources());
        List<CompletableFuture<List<PipeDTO>>> pipeFutures = ALL_PIPE_TYPES.stream()
                .map(type -> CompletableFuture.supplyAsync(() -> feignSupport.callOrNull(SOURCE_AREA_DEVICES,
                        () -> pipeAssetFeignClient.listPipesByType(type), degradedSources, false), feignExecutor))
                .toList();
        List<PipeDTO> areaPipes = new ArrayList<>();
        for (CompletableFuture<List<PipeDTO>> future : pipeFutures) {
            List<PipeDTO> part = joinQuietly(future, SOURCE_AREA_DEVICES, degradedSources);
            if (part != null) {
                part.stream().filter(p -> areaId.equals(p.getAreaId())).forEach(areaPipes::add);
            }
        }
        if (areaPipes.isEmpty()) {
            return Collections.emptyList();
        }

        List<PipeDTO> sample = areaPipes.stream().limit(MAX_AREA_PIPES).toList();
        List<CompletableFuture<List<DeviceDTO>>> deviceFutures = sample.stream()
                .map(pipe -> CompletableFuture.supplyAsync(() -> feignSupport.callOrNull(SOURCE_AREA_DEVICES,
                        () -> pipeAssetFeignClient.listDevicesByPipeId(pipe.getPipeId()), degradedSources, false),
                        feignExecutor))
                .toList();
        Set<String> codes = new LinkedHashSet<>();
        for (CompletableFuture<List<DeviceDTO>> future : deviceFutures) {
            List<DeviceDTO> devices = joinQuietly(future, SOURCE_AREA_DEVICES, degradedSources);
            if (devices == null) {
                continue;
            }
            devices.stream()
                    .map(DeviceDTO::getDeviceCode)
                    .filter(Objects::nonNull)
                    .forEach(codes::add);
            if (codes.size() >= MAX_TOTAL_DEVICES) {
                break;
            }
        }
        return codes.stream().limit(MAX_TOTAL_DEVICES).toList();
    }

    /**
     * 读取单页设备实时缓存并归并结果；整批 Pipeline 异常登记降级不中断。
     *
     * @param vo   结果视图
     * @param page 本页设备编码列表
     */
    private void readPage(RealtimeMappingVO vo, List<String> page) {
        List<DeviceRealtimeVO> batch;
        try {
            batch = realtimeCacheReader.readBatchPipelined(page);
        } catch (Exception e) {
            log.warn("Redis Pipeline批量读取失败, 本页跳过: size={}", page.size(), e);
            vo.getDegradedSources().add(SOURCE_REDIS_RT);
            vo.setMissed(vo.getMissed() + page.size());
            return;
        }
        for (DeviceRealtimeVO device : batch) {
            if (device == null) {
                vo.setMissed(vo.getMissed() + 1);
            } else {
                vo.setMatched(vo.getMatched() + 1);
                vo.getDevices().add(device);
            }
        }
    }

    /**
     * 带超时与异常吞没的 Future 汇合（超时/异常登记降级并返回 null）。
     *
     * @param future          待汇合 Future
     * @param source          数据源标识
     * @param degradedSources 降级来源登记列表
     * @param <T>             结果类型
     * @return Future 结果，失败降级返回 null
     */
    private <T> T joinQuietly(CompletableFuture<T> future, String source, List<String> degradedSources) {
        try {
            return future.get(1500L, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("聚合分片超时或异常, 降级处理: source={}", source, e);
            degradedSources.add(source);
            return null;
        }
    }
}
