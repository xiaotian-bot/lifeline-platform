package com.tianxin.lifeline.twin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianxin.lifeline.api.asset.PipeAssetFeignClient;
import com.tianxin.lifeline.api.asset.dto.DeviceDTO;
import com.tianxin.lifeline.api.asset.dto.PipeDTO;
import com.tianxin.lifeline.api.warning.WarningFeignClient;
import com.tianxin.lifeline.api.warning.dto.WarningEventDTO;
import com.tianxin.lifeline.common.core.exception.BizException;
import com.tianxin.lifeline.common.core.result.ErrorCode;
import com.tianxin.lifeline.twin.constant.TwinRedisKeys;
import com.tianxin.lifeline.twin.dto.DeviceRealtimeVO;
import com.tianxin.lifeline.twin.dto.WarningLocationItemVO;
import com.tianxin.lifeline.twin.dto.WarningLocationResultVO;
import com.tianxin.lifeline.twin.service.RealtimeCacheReader;
import com.tianxin.lifeline.twin.service.TwinFeignSupport;
import com.tianxin.lifeline.twin.service.TwinWarningLocationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * 预警定位服务实现。
 *
 * <p>定位坐标来源优先级：设备档案坐标（DEVICE）→ 管线 WKT 首点（PIPE）→ 无坐标（NONE）。
 * 结果按作用域缓存 Redis 1 分钟；降级结果不入缓存，避免脏数据驻留。
 */
@Slf4j
@Service
public class TwinWarningLocationServiceImpl implements TwinWarningLocationService {

    /** 结果缓存 TTL（1 分钟） */
    private static final Duration CACHE_TTL = Duration.ofMinutes(1);

    /** 定位来源：设备档案坐标 */
    private static final String LOCATION_SOURCE_DEVICE = "DEVICE";

    /** 定位来源：管线 WKT 首点 */
    private static final String LOCATION_SOURCE_PIPE = "PIPE";

    /** 定位来源：无坐标 */
    private static final String LOCATION_SOURCE_NONE = "NONE";

    /** 降级来源标识：预警服务 */
    private static final String SOURCE_WARNING = "warning";

    /** 降级来源标识：预警条目拼装（中性标识，不指向具体依赖） */
    private static final String SOURCE_ITEM_ASSEMBLE = "warning-item-assemble";

    /** 降级来源标识：管线资产 */
    private static final String SOURCE_PIPE = "pipe";

    /** 降级来源标识：设备资产 */
    private static final String SOURCE_DEVICE = "device";

    /** 预警预报 Feign 客户端 */
    private final WarningFeignClient warningFeignClient;

    /** 管网资产 Feign 客户端 */
    private final PipeAssetFeignClient pipeAssetFeignClient;

    /** Feign 降级支撑 */
    private final TwinFeignSupport feignSupport;

    /** 实时缓存读取器 */
    private final RealtimeCacheReader realtimeCacheReader;

    /** Redis 字符串模板（结果缓存） */
    private final StringRedisTemplate stringRedisTemplate;

    /** JSON 序列化器（缓存读写） */
    private final ObjectMapper objectMapper;

    /** 跨服务并行聚合线程池 */
    private final Executor feignExecutor;

    /**
     * 构造服务。
     *
     * @param warningFeignClient   预警预报 Feign 客户端
     * @param pipeAssetFeignClient 管网资产 Feign 客户端
     * @param feignSupport         Feign 降级支撑
     * @param realtimeCacheReader  实时缓存读取器
     * @param stringRedisTemplate  Redis 字符串模板
     * @param objectMapper         JSON 序列化器
     * @param feignExecutor        聚合线程池
     */
    public TwinWarningLocationServiceImpl(WarningFeignClient warningFeignClient,
                                          PipeAssetFeignClient pipeAssetFeignClient,
                                          TwinFeignSupport feignSupport,
                                          RealtimeCacheReader realtimeCacheReader,
                                          StringRedisTemplate stringRedisTemplate,
                                          ObjectMapper objectMapper,
                                          @Qualifier("twinFeignExecutor") Executor feignExecutor) {
        this.warningFeignClient = warningFeignClient;
        this.pipeAssetFeignClient = pipeAssetFeignClient;
        this.feignSupport = feignSupport;
        this.realtimeCacheReader = realtimeCacheReader;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.feignExecutor = feignExecutor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WarningLocationResultVO locate(Long warningId, Long pipeId, Long areaId) {
        if (warningId == null && pipeId == null && areaId == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "warningId、pipeId、areaId 至少提供其一");
        }
        String scope;
        if (warningId != null) {
            scope = "warning-" + warningId;
        } else if (pipeId != null) {
            scope = "pipe-" + pipeId;
        } else {
            scope = "area-" + areaId;
        }

        // 1. Redis 缓存优先
        WarningLocationResultVO cached = readCache(scope);
        if (cached != null) {
            return cached;
        }

        // 2. 拉取预警列表（单条 / 管线活跃 / 区域活跃）
        List<String> degradedSources = Collections.synchronizedList(new ArrayList<>());
        List<WarningEventDTO> warnings = loadWarnings(warningId, pipeId, areaId, degradedSources);

        // 3. 逐条并行拼装定位数据
        List<CompletableFuture<WarningLocationItemVO>> futures = warnings.stream()
                .map(warning -> CompletableFuture.supplyAsync(
                        () -> buildItem(warning, degradedSources), feignExecutor))
                .toList();
        WarningLocationResultVO vo = new WarningLocationResultVO();
        vo.setScope(scope);
        for (CompletableFuture<WarningLocationItemVO> future : futures) {
            WarningLocationItemVO item = joinQuietly(future, SOURCE_ITEM_ASSEMBLE, degradedSources);
            if (item != null) {
                vo.getItems().add(item);
            }
        }
        vo.setCount(vo.getItems().size());
        vo.setDegradedSources(degradedSources);
        vo.setDegraded(!degradedSources.isEmpty());

        // 4. 非降级结果写缓存（TTL 1 分钟）
        if (!vo.isDegraded()) {
            writeCache(scope, vo);
        }
        return vo;
    }

    /**
     * 拉取预警列表（失败降级为空列表）。
     *
     * @param warningId       预警ID（可空）
     * @param pipeId          管网ID（可空）
     * @param areaId          区域ID（可空）
     * @param degradedSources 降级来源登记列表
     * @return 预警列表（不为 null）
     */
    private List<WarningEventDTO> loadWarnings(Long warningId, Long pipeId, Long areaId,
                                               List<String> degradedSources) {
        if (warningId != null) {
            // 成功但预警不存在（data=null）属「成功无数据」，不登记降级
            WarningEventDTO warning = feignSupport.callOrNull(SOURCE_WARNING,
                    () -> warningFeignClient.getWarningById(warningId), degradedSources, false);
            return warning == null ? Collections.emptyList() : List.of(warning);
        }
        List<WarningEventDTO> warnings;
        if (pipeId != null) {
            warnings = feignSupport.callOrNull(SOURCE_WARNING,
                    () -> warningFeignClient.listActiveWarnings(pipeId), degradedSources);
        } else {
            warnings = feignSupport.callOrNull(SOURCE_WARNING,
                    () -> warningFeignClient.listActiveWarningsByArea(areaId), degradedSources);
        }
        return warnings == null ? Collections.emptyList() : warnings;
    }

    /**
     * 拼装单条预警定位条目（设备/管线位置 + 实时值）。
     *
     * @param warning         预警事件
     * @param degradedSources 降级来源登记列表
     * @return 定位条目
     */
    private WarningLocationItemVO buildItem(WarningEventDTO warning, List<String> degradedSources) {
        WarningLocationItemVO item = new WarningLocationItemVO();
        item.setWarningId(warning.getWarningId());
        item.setWarningNo(warning.getWarningNo());
        item.setWarningLevel(warning.getWarningLevel());
        item.setTitle(warning.getTitle());
        item.setStatus(warning.getStatus());
        item.setPipeType(warning.getPipeType());
        item.setPipeId(warning.getPipeId());
        item.setDeviceId(warning.getDeviceId());

        // 管线信息（名称与 WKT 坐标兜底；成功但管线不存在不登记降级）
        PipeDTO pipe = warning.getPipeId() == null ? null
                : feignSupport.callOrNull(SOURCE_PIPE,
                        () -> pipeAssetFeignClient.getPipeById(warning.getPipeId()), degradedSources, false);
        if (pipe != null) {
            item.setPipeName(pipe.getPipeName());
        }

        // 设备信息优先（坐标 + 设备编码；成功但设备不存在不登记降级）
        DeviceDTO device = warning.getDeviceId() == null ? null
                : feignSupport.callOrNull(SOURCE_DEVICE,
                        () -> pipeAssetFeignClient.getDeviceById(warning.getDeviceId()), degradedSources, false);
        if (device != null) {
            item.setDeviceCode(device.getDeviceCode());
        }
        if (device != null && device.getLongitude() != null && device.getLatitude() != null) {
            item.setLongitude(device.getLongitude());
            item.setLatitude(device.getLatitude());
            item.setLocationSource(LOCATION_SOURCE_DEVICE);
        } else if (pipe != null && pipe.getGeometryWkt() != null) {
            double[] point = parseFirstPoint(pipe.getGeometryWkt());
            if (point != null) {
                item.setLongitude(point[0]);
                item.setLatitude(point[1]);
                item.setLocationSource(LOCATION_SOURCE_PIPE);
            } else {
                item.setLocationSource(LOCATION_SOURCE_NONE);
            }
        } else {
            item.setLocationSource(LOCATION_SOURCE_NONE);
        }

        // 实时值（预警携带设备编码时经 Redis 读取，缺失不降级）
        if (item.getDeviceCode() != null) {
            DeviceRealtimeVO realtime = realtimeCacheReader.readOne(item.getDeviceCode());
            if (realtime != null) {
                item.setRealtimeStatus(realtime.getStatus());
                item.setRealtimeTs(realtime.getTs());
            }
        }
        return item;
    }

    /**
     * 解析 WKT 线串首点坐标。
     *
     * @param wkt WKT 文本（如 LINESTRING(lng lat, lng lat, ...)，兼容 SRID 前缀）
     * @return [经度, 纬度]，解析失败返回 null
     */
    private double[] parseFirstPoint(String wkt) {
        try {
            String text = wkt;
            int sridEnd = text.indexOf(';');
            if (text.startsWith("SRID=") && sridEnd > 0) {
                text = text.substring(sridEnd + 1);
            }
            int start = text.indexOf('(');
            int end = text.indexOf(')', start);
            if (start < 0 || end <= start) {
                return null;
            }
            String first = text.substring(start + 1, end).split(",")[0].trim();
            String[] parts = first.split("\\s+");
            return new double[]{Double.parseDouble(parts[0]), Double.parseDouble(parts[1])};
        } catch (Exception e) {
            log.warn("WKT首点解析失败: wkt={}", wkt, e);
            return null;
        }
    }

    /**
     * 读取结果缓存（失败忽略走实时聚合）。
     *
     * @param scope 作用域标识
     * @return 缓存结果，未命中或解析失败返回 null
     */
    private WarningLocationResultVO readCache(String scope) {
        try {
            String json = stringRedisTemplate.opsForValue().get(TwinRedisKeys.warningLocationKey(scope));
            if (json == null || json.isBlank()) {
                return null;
            }
            WarningLocationResultVO vo = objectMapper.readValue(json, WarningLocationResultVO.class);
            vo.setCached(true);
            return vo;
        } catch (Exception e) {
            log.warn("预警定位缓存读取失败, 走实时聚合: scope={}", scope, e);
            return null;
        }
    }

    /**
     * 写入结果缓存（TTL 1 分钟，失败仅日志）。
     *
     * @param scope 作用域标识
     * @param vo    聚合结果
     */
    private void writeCache(String scope, WarningLocationResultVO vo) {
        try {
            String json = objectMapper.writeValueAsString(vo);
            stringRedisTemplate.opsForValue()
                    .set(TwinRedisKeys.warningLocationKey(scope), json, CACHE_TTL);
        } catch (Exception e) {
            log.warn("预警定位缓存写入失败: scope={}", scope, e);
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
