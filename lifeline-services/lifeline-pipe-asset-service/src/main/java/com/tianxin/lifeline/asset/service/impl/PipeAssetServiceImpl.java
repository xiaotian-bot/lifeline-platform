package com.tianxin.lifeline.asset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianxin.lifeline.api.asset.dto.DeviceDTO;
import com.tianxin.lifeline.api.asset.dto.PipeDTO;
import com.tianxin.lifeline.asset.converter.AssetDtoConverter;
import com.tianxin.lifeline.asset.dto.GeoJsonResponse;
import com.tianxin.lifeline.asset.dto.PipeArchiveResponse;
import com.tianxin.lifeline.asset.dto.PipeCreateRequest;
import com.tianxin.lifeline.asset.dto.PipeDetailResponse;
import com.tianxin.lifeline.asset.dto.PipePageRequest;
import com.tianxin.lifeline.asset.entity.Manhole;
import com.tianxin.lifeline.asset.entity.PipeAttribute;
import com.tianxin.lifeline.asset.entity.PipeDevice;
import com.tianxin.lifeline.asset.entity.PipeMain;
import com.tianxin.lifeline.asset.entity.PipeTopology;
import com.tianxin.lifeline.asset.mapper.ManholeMapper;
import com.tianxin.lifeline.asset.mapper.PipeAttributeMapper;
import com.tianxin.lifeline.asset.mapper.PipeDeviceMapper;
import com.tianxin.lifeline.asset.mapper.PipeMainMapper;
import com.tianxin.lifeline.asset.mapper.PipeTopologyMapper;
import com.tianxin.lifeline.asset.service.PipeAssetService;
import com.tianxin.lifeline.common.core.exception.BizException;
import com.tianxin.lifeline.common.core.id.SnowflakeIdGenerator;
import com.tianxin.lifeline.common.core.page.PageResult;
import com.tianxin.lifeline.common.core.result.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管网资产服务实现。
 *
 * <p>几何读写全部经 PostGIS SQL 函数（ST_AsText/ST_AsGeoJSON/ST_GeomFromText），
 * 不依赖 postgis-jdbc；GeoJSON 结果使用 Redis 缓存，写操作后主动失效。
 */
@Slf4j
@Service
public class PipeAssetServiceImpl implements PipeAssetService {

    /** GeoJSON 缓存键前缀（决策口径 2：area_id 缺失，缓存键降级为按 pipeType） */
    private static final String GEOJSON_CACHE_PREFIX = "asset:geojson:";

    /** 全量 GeoJSON 缓存键后缀 */
    private static final String GEOJSON_CACHE_ALL = "all";

    /** GeoJSON 缓存有效期：10 分钟 */
    private static final Duration GEOJSON_CACHE_TTL = Duration.ofMinutes(10);

    /** 一管一档中井室邻近聚合半径（米） */
    private static final double MANHOLE_NEAR_RADIUS = 200.0;

    /** 管网主线 Mapper */
    private final PipeMainMapper pipeMainMapper;

    /** 管网扩展属性 Mapper */
    private final PipeAttributeMapper pipeAttributeMapper;

    /** 管网设备 Mapper */
    private final PipeDeviceMapper pipeDeviceMapper;

    /** 井室 Mapper */
    private final ManholeMapper manholeMapper;

    /** 管网拓扑 Mapper */
    private final PipeTopologyMapper pipeTopologyMapper;

    /** Redis 模板（GeoJSON 缓存） */
    private final RedisTemplate<String, Object> redisTemplate;

    /** JSON 工具（解析 ST_AsGeoJSON 输出） */
    private final ObjectMapper objectMapper;

    /**
     * 构造管网资产服务。
     *
     * @param pipeMainMapper      管网主线 Mapper
     * @param pipeAttributeMapper 管网扩展属性 Mapper
     * @param pipeDeviceMapper    管网设备 Mapper
     * @param manholeMapper       井室 Mapper
     * @param pipeTopologyMapper  管网拓扑 Mapper
     * @param redisTemplate       Redis 模板
     * @param objectMapper        JSON 工具
     */
    public PipeAssetServiceImpl(PipeMainMapper pipeMainMapper,
                                PipeAttributeMapper pipeAttributeMapper,
                                PipeDeviceMapper pipeDeviceMapper,
                                ManholeMapper manholeMapper,
                                PipeTopologyMapper pipeTopologyMapper,
                                RedisTemplate<String, Object> redisTemplate,
                                ObjectMapper objectMapper) {
        this.pipeMainMapper = pipeMainMapper;
        this.pipeAttributeMapper = pipeAttributeMapper;
        this.pipeDeviceMapper = pipeDeviceMapper;
        this.manholeMapper = manholeMapper;
        this.pipeTopologyMapper = pipeTopologyMapper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult<PipeDTO> pagePipes(PipePageRequest request) {
        Page<PipeMain> page = new Page<>(request.getPage(), request.getSize());
        // 决策口径 1：pipe_type 入参大小写不敏感，归一为 DB 小写编码
        String pipeType = AssetDtoConverter.normalizePipeType(request.getPipeType());
        IPage<PipeMain> result = pipeMainMapper.selectPipePage(page, pipeType, request.getArea(), request.getStatus());
        List<PipeDTO> records = result.getRecords().stream().map(AssetDtoConverter::toPipeDto).toList();
        return PageResult.of(records, result.getTotal(), request.getPage(), request.getSize());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PipeDTO getPipeById(Long pipeId) {
        PipeMain pipe = pipeMainMapper.selectDetailById(pipeId);
        if (pipe == null) {
            throw new BizException(ErrorCode.PIPE_NOT_FOUND);
        }
        return AssetDtoConverter.toPipeDto(pipe);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PipeDetailResponse getPipeDetail(Long pipeId) {
        PipeMain pipe = pipeMainMapper.selectDetailById(pipeId);
        if (pipe == null) {
            throw new BizException(ErrorCode.PIPE_NOT_FOUND);
        }
        PipeDetailResponse response = new PipeDetailResponse();
        response.setId(pipe.getId());
        response.setPipeCode(pipe.getPipeCode());
        response.setPipeName(pipe.getPipeName());
        response.setPipeType(pipe.getPipeType());
        response.setDiameter(pipe.getDiameter());
        response.setMaterial(pipe.getMaterial());
        response.setLength(pipe.getLength());
        response.setDepth(pipe.getDepth());
        response.setInstallDate(pipe.getInstallDate());
        response.setStatus(pipe.getStatus());
        response.setGeomWkt(pipe.getGeomWkt());
        response.setCreatedAt(pipe.getCreatedAt());
        response.setUpdatedAt(pipe.getUpdatedAt());
        response.setAttributes(pipeAttributeMapper.selectByPipeId(pipeId));
        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<PipeDTO> listPipesByType(String pipeType) {
        // 决策口径 1：大小写不敏感 + 同义词归一；枚举命中失败的编码原样透传不阻断
        String normalized = AssetDtoConverter.normalizePipeType(pipeType);
        return pipeMainMapper.selectByType(normalized).stream().map(AssetDtoConverter::toPipeDto).toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPipe(PipeCreateRequest request) {
        // pipeCode 唯一性校验（deleted=0 范围内）
        Long count = pipeMainMapper.selectCount(new LambdaQueryWrapper<PipeMain>()
                .eq(PipeMain::getPipeCode, request.getPipeCode()));
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "管网编码已存在：" + request.getPipeCode());
        }

        LocalDateTime now = LocalDateTime.now();
        PipeMain pipe = new PipeMain();
        // 自定义 XML 插入不走 MyBatis-Plus 主键与字段填充，手工生成雪花ID与时间戳
        pipe.setId(SnowflakeIdGenerator.getInstance().nextId());
        pipe.setPipeCode(request.getPipeCode());
        pipe.setPipeName(request.getPipeName());
        pipe.setPipeType(AssetDtoConverter.normalizePipeType(request.getPipeType()));
        pipe.setDiameter(request.getDiameter());
        pipe.setMaterial(request.getMaterial());
        pipe.setLength(request.getLength());
        pipe.setDepth(request.getDepth());
        pipe.setInstallDate(request.getInstallDate());
        pipe.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        pipe.setGeomWkt(request.getGeomWkt());
        pipe.setCreatedAt(now);
        pipe.setUpdatedAt(now);
        pipeMainMapper.insertPipe(pipe);

        // 扩展属性 KV 写入 pipe_attribute
        saveAttributes(pipe.getId(), request.getAttributes(), now);

        // 写操作后失效 GeoJSON 缓存
        evictGeoJsonCache(pipe.getPipeType());
        log.info("新增管网成功 id={}, pipeCode={}", pipe.getId(), pipe.getPipeCode());
        return pipe.getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePipe(Long pipeId, PipeCreateRequest request) {
        PipeMain existing = pipeMainMapper.selectDetailById(pipeId);
        if (existing == null) {
            throw new BizException(ErrorCode.PIPE_NOT_FOUND);
        }
        // 变更 pipeCode 时校验唯一性
        if (request.getPipeCode() != null && !request.getPipeCode().equals(existing.getPipeCode())) {
            Long count = pipeMainMapper.selectCount(new LambdaQueryWrapper<PipeMain>()
                    .eq(PipeMain::getPipeCode, request.getPipeCode()));
            if (count != null && count > 0) {
                throw new BizException(ErrorCode.PARAM_INVALID, "管网编码已存在：" + request.getPipeCode());
            }
        }

        PipeMain pipe = new PipeMain();
        pipe.setId(pipeId);
        pipe.setPipeCode(request.getPipeCode());
        pipe.setPipeName(request.getPipeName());
        pipe.setPipeType(AssetDtoConverter.normalizePipeType(request.getPipeType()));
        pipe.setDiameter(request.getDiameter());
        pipe.setMaterial(request.getMaterial());
        pipe.setLength(request.getLength());
        pipe.setDepth(request.getDepth());
        pipe.setInstallDate(request.getInstallDate());
        pipe.setStatus(request.getStatus());
        pipe.setGeomWkt(request.getGeomWkt());
        pipe.setUpdatedAt(LocalDateTime.now());
        pipeMainMapper.updatePipe(pipe);

        // 扩展属性全量替换：请求携带 attributes 时先逻辑删除旧值再批量写入
        if (request.getAttributes() != null) {
            pipeAttributeMapper.logicDeleteByPipeId(pipeId);
            saveAttributes(pipeId, request.getAttributes(), pipe.getUpdatedAt());
        }

        // 写操作后失效 GeoJSON 缓存（新旧类型均失效）
        evictGeoJsonCache(existing.getPipeType());
        if (pipe.getPipeType() != null && !pipe.getPipeType().equals(existing.getPipeType())) {
            evictGeoJsonCache(pipe.getPipeType());
        }
        log.info("更新管网成功 id={}", pipeId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GeoJsonResponse getGeoJson(String pipeType) {
        String normalized = AssetDtoConverter.normalizePipeType(pipeType);
        String cacheKey = GEOJSON_CACHE_PREFIX + (normalized == null ? GEOJSON_CACHE_ALL : normalized);

        // 命中缓存直接返回（Redis 故障时降级为查库，不阻断接口）
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof GeoJsonResponse cachedResponse) {
                return cachedResponse;
            }
        } catch (Exception e) {
            log.warn("读取 GeoJSON 缓存失败，降级查库 key={}", cacheKey, e);
        }

        GeoJsonResponse response = buildGeoJson(normalized);

        try {
            redisTemplate.opsForValue().set(cacheKey, response, GEOJSON_CACHE_TTL);
        } catch (Exception e) {
            log.warn("写入 GeoJSON 缓存失败 key={}", cacheKey, e);
        }
        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PipeArchiveResponse getArchive(Long pipeId) {
        PipeMain pipe = pipeMainMapper.selectDetailById(pipeId);
        if (pipe == null) {
            throw new BizException(ErrorCode.PIPE_NOT_FOUND);
        }

        PipeArchiveResponse archive = new PipeArchiveResponse();
        archive.setPipe(AssetDtoConverter.toPipeDto(pipe));
        archive.setAttributes(pipeAttributeMapper.selectByPipeId(pipeId));

        List<PipeDevice> devices = pipeDeviceMapper.selectByPipeId(pipeId);
        archive.setDevices(devices.stream().map(AssetDtoConverter::toDeviceDto).toList());

        // manhole 表无 pipe_id 外键，按空间邻近半径聚合关联井室
        archive.setManholes(manholeMapper.selectNearPipe(pipeId, MANHOLE_NEAR_RADIUS));

        List<PipeTopology> topologies = pipeTopologyMapper.selectByPipeId(pipeId);
        archive.setTopologies(topologies);
        return archive;
    }

    /**
     * 组装 GeoJSON FeatureCollection（RFC 7946）。
     *
     * <p>geometry 由 ST_AsGeoJSON 输出后经 ObjectMapper 解析为嵌套对象；
     * properties 含 pipeId/pipeCode/pipeName/pipeType/status。
     *
     * @param pipeType 归一化后的管网类型（可空）
     * @return GeoJSON 响应
     */
    private GeoJsonResponse buildGeoJson(String pipeType) {
        List<PipeMain> pipes = pipeMainMapper.selectGeoJsonList(pipeType);
        GeoJsonResponse response = new GeoJsonResponse();
        for (PipeMain pipe : pipes) {
            Map<String, Object> feature = new HashMap<>();
            feature.put("type", "Feature");
            feature.put("geometry", parseGeometry(pipe.getGeomGeoJson()));
            Map<String, Object> properties = new HashMap<>();
            properties.put("pipeId", pipe.getId());
            properties.put("pipeCode", pipe.getPipeCode());
            properties.put("pipeName", pipe.getPipeName());
            properties.put("pipeType", pipe.getPipeType());
            properties.put("status", AssetDtoConverter.pipeStatusText(pipe.getStatus()));
            feature.put("properties", properties);
            response.getFeatures().add(feature);
        }
        return response;
    }

    /**
     * 解析 ST_AsGeoJSON 输出的几何 JSON 文本为 Map。
     *
     * @param geoJson 几何 JSON 文本
     * @return 几何对象 Map，解析失败或为空返回 null
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseGeometry(String geoJson) {
        if (geoJson == null || geoJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(geoJson, Map.class);
        } catch (Exception e) {
            log.warn("解析 GeoJSON 几何失败: {}", geoJson, e);
            return null;
        }
    }

    /**
     * 批量写入扩展属性 KV。
     *
     * @param pipeId     管网ID
     * @param attributes 属性列表（每项为 {key,value,unit}，或若干 键→值 键值对）
     * @param now        当前时间
     */
    private void saveAttributes(Long pipeId, List<Map<String, String>> attributes, LocalDateTime now) {
        if (attributes == null || attributes.isEmpty()) {
            return;
        }
        List<PipeAttribute> list = new ArrayList<>();
        for (Map<String, String> attr : attributes) {
            if (attr == null || attr.isEmpty()) {
                continue;
            }
            if (attr.containsKey("key")) {
                // 结构化形式：{key: 属性键, value: 属性值, unit: 单位}
                list.add(buildAttribute(pipeId, attr.get("key"), attr.get("value"), attr.get("unit"), now));
            } else {
                // 扁平形式：每个键值对作为一条属性
                for (Map.Entry<String, String> entry : attr.entrySet()) {
                    list.add(buildAttribute(pipeId, entry.getKey(), entry.getValue(), null, now));
                }
            }
        }
        if (!list.isEmpty()) {
            pipeAttributeMapper.batchInsert(list);
        }
    }

    /**
     * 构造单条扩展属性实体（雪花ID与时间戳预填充）。
     *
     * @param pipeId 管网ID
     * @param key    属性键
     * @param value  属性值
     * @param unit   属性单位（可空）
     * @param now    当前时间
     * @return 扩展属性实体
     */
    private PipeAttribute buildAttribute(Long pipeId, String key, String value, String unit, LocalDateTime now) {
        PipeAttribute attribute = new PipeAttribute();
        attribute.setId(SnowflakeIdGenerator.getInstance().nextId());
        attribute.setPipeId(pipeId);
        attribute.setAttrKey(key);
        attribute.setAttrValue(value);
        attribute.setAttrUnit(unit);
        attribute.setCreatedAt(now);
        attribute.setUpdatedAt(now);
        return attribute;
    }

    /**
     * 失效 GeoJSON 缓存（指定类型键 + 全量键）。
     *
     * @param pipeType 管网类型（可空，仅失效全量键）
     */
    private void evictGeoJsonCache(String pipeType) {
        try {
            List<String> keys = new ArrayList<>();
            keys.add(GEOJSON_CACHE_PREFIX + GEOJSON_CACHE_ALL);
            if (pipeType != null && !pipeType.isBlank()) {
                keys.add(GEOJSON_CACHE_PREFIX + pipeType);
            }
            redisTemplate.delete(keys);
        } catch (Exception e) {
            log.warn("失效 GeoJSON 缓存失败 pipeType={}", pipeType, e);
        }
    }
}
