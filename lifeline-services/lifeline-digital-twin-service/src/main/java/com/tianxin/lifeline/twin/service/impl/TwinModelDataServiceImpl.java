package com.tianxin.lifeline.twin.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.tianxin.lifeline.api.asset.PipeAssetFeignClient;
import com.tianxin.lifeline.api.asset.dto.DeviceDTO;
import com.tianxin.lifeline.api.asset.dto.GeoJsonDTO;
import com.tianxin.lifeline.api.asset.dto.PipeDTO;
import com.tianxin.lifeline.twin.config.TwinAsyncConfig;
import com.tianxin.lifeline.twin.dto.ModelDataVO;
import com.tianxin.lifeline.twin.service.TwinFeignSupport;
import com.tianxin.lifeline.twin.service.TwinModelDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * 模型数据聚合服务实现。
 *
 * <p>编排策略：管线列表与 GeoJSON 并行拉取（twinFeignExecutor），
 * 设备统计在管线列表就绪后按前 20 条管线并行展开；单依赖失败仅降级对应分片。
 * 结果经 Caffeine L1 缓存 30 秒，整体目标 ≤1s（聚合超时 1.5s 兜底）。
 */
@Slf4j
@Service
public class TwinModelDataServiceImpl implements TwinModelDataService {

    /** 全量管网类型编码（pipeType 为空时逐一并行拉取） */
    private static final List<String> ALL_PIPE_TYPES = List.of("WATER", "HEAT", "GAS", "SEWAGE", "HAZARD");

    /** 设备统计聚合的管线数量上限（超出部分截断，保证接口时延） */
    private static final int MAX_DEVICE_AGGREGATE_PIPES = 20;

    /** GeoJSON 概要返回的 Feature 上限（控制响应体积） */
    private static final int GEO_FEATURE_SUMMARY_LIMIT = 20;

    /** 聚合整体超时（毫秒，兜底防止单依赖拖垮接口） */
    private static final long AGGREGATE_TIMEOUT_MS = 1500L;

    /** 井盖设备类型标识（pipe_device.device_type） */
    private static final String MANHOLE_DEVICE_TYPE = "井盖";

    /** 降级来源标识：管线列表 */
    private static final String SOURCE_PIPE_LIST = "pipe-list";

    /** 降级来源标识：设备列表 */
    private static final String SOURCE_DEVICE_LIST = "device-list";

    /** 降级来源标识：GeoJSON */
    private static final String SOURCE_GEOJSON = "geojson";

    /** 管网资产 Feign 客户端 */
    private final PipeAssetFeignClient pipeAssetFeignClient;

    /** Feign 降级支撑 */
    private final TwinFeignSupport feignSupport;

    /** 跨服务并行聚合线程池 */
    private final Executor feignExecutor;

    /** 模型数据 Caffeine L1 缓存（30 秒过期） */
    private final Cache<String, Object> modelDataCache;

    /**
     * 构造服务。
     *
     * @param pipeAssetFeignClient 管网资产 Feign 客户端
     * @param feignSupport         Feign 降级支撑
     * @param feignExecutor        聚合线程池
     * @param modelDataCache       模型数据 L1 缓存
     */
    public TwinModelDataServiceImpl(PipeAssetFeignClient pipeAssetFeignClient,
                                    TwinFeignSupport feignSupport,
                                    @Qualifier("twinFeignExecutor") Executor feignExecutor,
                                    @Qualifier(TwinAsyncConfig.MODEL_DATA_CACHE) Cache<String, Object> modelDataCache) {
        this.pipeAssetFeignClient = pipeAssetFeignClient;
        this.feignSupport = feignSupport;
        this.feignExecutor = feignExecutor;
        this.modelDataCache = modelDataCache;
    }

    /**
     * {@inheritDoc}
     *
     * <p>L1 缓存键为 pipeType:areaId 组合（归一化：空值统一 ALL、非空 trim），
     * 30 秒内重复请求直接返回缓存；降级结果不入缓存，避免脏数据驻留。
     */
    @Override
    public ModelDataVO getModelData(String pipeType, Long areaId) {
        String cacheKey = normalizeCacheKey(pipeType, areaId);
        Object cached = modelDataCache.getIfPresent(cacheKey);
        if (cached instanceof ModelDataVO cachedVo) {
            return cachedVo;
        }
        ModelDataVO vo = load(pipeType, areaId);
        // 与 warning-location 约定一致：降级结果不入缓存
        if (!vo.isDegraded()) {
            modelDataCache.put(cacheKey, vo);
        }
        return vo;
    }

    /**
     * 缓存键归一化：pipeType 为 null/空白统一为 ALL，非空 trim，避免键分裂与字面量碰撞。
     *
     * @param pipeType 管网类型（可空）
     * @param areaId   区域ID（可空）
     * @return 归一化缓存键
     */
    private String normalizeCacheKey(String pipeType, Long areaId) {
        String type = (pipeType == null || pipeType.isBlank()) ? "ALL" : pipeType.trim();
        String area = areaId == null ? "ALL" : String.valueOf(areaId);
        return type + ":" + area;
    }

    /**
     * 实际聚合加载（缓存未命中时执行）。
     *
     * @param pipeType 管网类型（可空）
     * @param areaId   区域ID（可空）
     * @return 模型数据聚合视图（永不返回 null）
     */
    private ModelDataVO load(String pipeType, Long areaId) {
        ModelDataVO vo = new ModelDataVO();
        vo.setPipeType(pipeType);
        vo.setAreaId(areaId);
        List<String> degradedSources = Collections.synchronizedList(new ArrayList<>());

        // 管线列表与 GeoJSON 并行拉取（成功无数据不登记降级）
        CompletableFuture<List<PipeDTO>> pipesFuture = CompletableFuture
                .supplyAsync(() -> loadPipes(pipeType, areaId, degradedSources), feignExecutor);
        CompletableFuture<GeoJsonDTO> geoFuture = CompletableFuture
                .supplyAsync(() -> feignSupport.callOrNull(SOURCE_GEOJSON,
                        () -> pipeAssetFeignClient.getGeoJson(pipeType), degradedSources, false), feignExecutor);

        List<PipeDTO> pipes = joinQuietly(pipesFuture, SOURCE_PIPE_LIST, degradedSources);
        GeoJsonDTO geoJson = joinQuietly(geoFuture, SOURCE_GEOJSON, degradedSources);

        // 管线统计（长度求和）
        if (pipes != null) {
            vo.setPipeCount(pipes.size());
            vo.setTotalLengthMeters(pipes.stream()
                    .filter(p -> p.getLength() != null)
                    .mapToDouble(PipeDTO::getLength)
                    .sum());
        }

        // 设备/井盖统计：按前 N 条管线并行展开
        if (pipes != null && !pipes.isEmpty()) {
            aggregateDevices(vo, pipes, degradedSources);
        }

        // GeoJSON 概要
        if (geoJson != null && geoJson.getFeatures() != null) {
            vo.setGeoFeatureCount(geoJson.getFeatures().size());
            vo.setGeoFeatureSummary(geoJson.getFeatures().stream()
                    .limit(GEO_FEATURE_SUMMARY_LIMIT)
                    .toList());
        }

        vo.setDegradedSources(degradedSources);
        vo.setDegraded(!degradedSources.isEmpty());
        return vo;
    }

    /**
     * 拉取管线列表（指定类型单次调用；未指定类型时五类并行合并），并按区域过滤。
     *
     * @param pipeType        管网类型（可空）
     * @param areaId          区域ID（可空）
     * @param degradedSources 降级来源登记列表
     * @return 管线列表，失败降级返回 null
     */
    private List<PipeDTO> loadPipes(String pipeType, Long areaId, List<String> degradedSources) {
        List<PipeDTO> pipes;
        if (pipeType != null && !pipeType.isBlank()) {
            pipes = feignSupport.callOrNull(SOURCE_PIPE_LIST,
                    () -> pipeAssetFeignClient.listPipesByType(pipeType), degradedSources, false);
        } else {
            List<CompletableFuture<List<PipeDTO>>> futures = ALL_PIPE_TYPES.stream()
                    .map(type -> CompletableFuture.supplyAsync(() -> feignSupport.callOrNull(SOURCE_PIPE_LIST,
                            () -> pipeAssetFeignClient.listPipesByType(type), degradedSources, false), feignExecutor))
                    .toList();
            pipes = new ArrayList<>();
            for (CompletableFuture<List<PipeDTO>> future : futures) {
                List<PipeDTO> part = joinQuietly(future, SOURCE_PIPE_LIST, degradedSources);
                if (part != null) {
                    pipes.addAll(part);
                }
            }
        }
        if (pipes == null) {
            return null;
        }
        if (areaId == null) {
            return pipes;
        }
        return pipes.stream()
                .filter(p -> areaId.equals(p.getAreaId()))
                .toList();
    }

    /**
     * 设备统计聚合：取前 {@value #MAX_DEVICE_AGGREGATE_PIPES} 条管线并行查询挂载设备，
     * 统计设备总数与井盖数量。
     *
     * @param vo              目标视图（回写 deviceCount/manholeCount）
     * @param pipes           管线列表
     * @param degradedSources 降级来源登记列表
     */
    private void aggregateDevices(ModelDataVO vo, List<PipeDTO> pipes, List<String> degradedSources) {
        List<PipeDTO> sample = pipes.stream().limit(MAX_DEVICE_AGGREGATE_PIPES).toList();
        List<CompletableFuture<List<DeviceDTO>>> futures = sample.stream()
                .map(pipe -> CompletableFuture.supplyAsync(() -> feignSupport.callOrNull(SOURCE_DEVICE_LIST,
                        () -> pipeAssetFeignClient.listDevicesByPipeId(pipe.getPipeId()), degradedSources, false),
                        feignExecutor))
                .toList();
        long deviceCount = 0L;
        long manholeCount = 0L;
        for (CompletableFuture<List<DeviceDTO>> future : futures) {
            List<DeviceDTO> devices = joinQuietly(future, SOURCE_DEVICE_LIST, degradedSources);
            if (devices == null) {
                continue;
            }
            deviceCount += devices.size();
            manholeCount += devices.stream()
                    .filter(d -> MANHOLE_DEVICE_TYPE.equals(d.getDeviceType()))
                    .count();
        }
        vo.setDeviceCount(deviceCount);
        vo.setManholeCount(manholeCount);
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
            return future.get(AGGREGATE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("聚合分片超时或异常, 降级处理: source={}", source, e);
            degradedSources.add(source);
            return null;
        }
    }
}
