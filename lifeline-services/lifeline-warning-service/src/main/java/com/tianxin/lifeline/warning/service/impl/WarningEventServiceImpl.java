package com.tianxin.lifeline.warning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianxin.lifeline.api.warning.dto.WarningEventDTO;
import com.tianxin.lifeline.common.core.constant.WarningLevel;
import com.tianxin.lifeline.common.core.exception.BizException;
import com.tianxin.lifeline.common.core.id.SnowflakeIdGenerator;
import com.tianxin.lifeline.common.core.page.PageResult;
import com.tianxin.lifeline.common.core.result.ErrorCode;
import com.tianxin.lifeline.common.security.context.SecurityContext;
import com.tianxin.lifeline.warning.config.WarningProperties;
import com.tianxin.lifeline.warning.constant.WarningRedisKeys;
import com.tianxin.lifeline.warning.constant.WarningSource;
import com.tianxin.lifeline.warning.constant.WarningStatus;
import com.tianxin.lifeline.warning.dto.WarningEventPayload;
import com.tianxin.lifeline.warning.dto.WarningEventVO;
import com.tianxin.lifeline.warning.dto.WarningQueryRequest;
import com.tianxin.lifeline.warning.dto.WarningStatisticsVO;
import com.tianxin.lifeline.warning.entity.WarningEvent;
import com.tianxin.lifeline.warning.mapper.WarningEventMapper;
import com.tianxin.lifeline.warning.service.AiJudgeBackfillService;
import com.tianxin.lifeline.warning.service.WarningEventService;
import com.tianxin.lifeline.warning.service.WarningPushService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 预警事件业务服务实现。
 *
 * <p>幂等落库三重保障（REST 与 Kafka 两个创建入口共用）：
 * <ol>
 *     <li>Redis SETNX：warningNo 级抢占（TTL 可配置，默认 10 分钟）；</li>
 *     <li>重复命中时回查已存在记录并直接返回（幂等语义）；</li>
 *     <li>数据库部分唯一索引 uk_warning_event_code 兜底并发竞争。</li>
 * </ol>
 *
 * <p>状态机：0 待确认 → 1 已确认（confirm）；0/1/2 → 3 已核销（cancel）。
 */
@Slf4j
@Service
public class WarningEventServiceImpl implements WarningEventService {

    /** 时间过滤参数格式 */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 预警事件 Mapper */
    private final WarningEventMapper warningEventMapper;

    /** 预警推送服务 */
    private final WarningPushService warningPushService;

    /** Redis 模板（幂等去重） */
    private final StringRedisTemplate stringRedisTemplate;

    /** 预警业务配置 */
    private final WarningProperties warningProperties;

    /** AI 研判异步回填服务（落库后异步深度研判，不阻塞主链路） */
    private final AiJudgeBackfillService aiJudgeBackfillService;

    /**
     * 构造预警事件服务。
     *
     * @param warningEventMapper     预警事件 Mapper
     * @param warningPushService     预警推送服务
     * @param stringRedisTemplate    Redis 模板
     * @param warningProperties      预警业务配置
     * @param aiJudgeBackfillService AI 研判异步回填服务
     */
    public WarningEventServiceImpl(WarningEventMapper warningEventMapper,
                                   WarningPushService warningPushService,
                                   StringRedisTemplate stringRedisTemplate,
                                   WarningProperties warningProperties,
                                   AiJudgeBackfillService aiJudgeBackfillService) {
        this.warningEventMapper = warningEventMapper;
        this.warningPushService = warningPushService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.warningProperties = warningProperties;
        this.aiJudgeBackfillService = aiJudgeBackfillService;
    }

    /**
     * 分页查询预警事件（状态输出中文）。
     *
     * @param request 分页与过滤条件
     * @return 分页结果
     */
    @Override
    public PageResult<WarningEventVO> page(WarningQueryRequest request) {
        LambdaQueryWrapper<WarningEvent> wrapper = new LambdaQueryWrapper<>();
        Integer level = parseLevel(request.getLevel());
        if (level != null) {
            wrapper.eq(WarningEvent::getWarningLevel, level);
        }
        Integer status = parseStatus(request.getStatus());
        if (status != null) {
            wrapper.eq(WarningEvent::getStatus, status);
        }
        if (request.getPipeType() != null && !request.getPipeType().isBlank()) {
            wrapper.eq(WarningEvent::getPipeType, request.getPipeType().trim());
        }
        if (request.getPipeId() != null) {
            wrapper.eq(WarningEvent::getPipeId, request.getPipeId());
        }
        if (request.getDeviceId() != null) {
            wrapper.eq(WarningEvent::getDeviceId, request.getDeviceId());
        }
        LocalDateTime start = parseTime(request.getStartTime());
        if (start != null) {
            wrapper.ge(WarningEvent::getCreatedAt, start);
        }
        LocalDateTime end = parseTime(request.getEndTime());
        if (end != null) {
            wrapper.le(WarningEvent::getCreatedAt, end);
        }
        wrapper.orderByDesc(WarningEvent::getCreatedAt);

        Page<WarningEvent> page = new Page<>(request.getPage(), request.getSize());
        Page<WarningEvent> result = warningEventMapper.selectPage(page, wrapper);
        List<WarningEventVO> records = result.getRecords().stream().map(this::toVo).toList();
        return PageResult.of(records, result.getTotal(), request.getPage(), request.getSize());
    }

    /**
     * 查询预警事件详情。
     *
     * @param warningId 预警事件ID
     * @return 预警事件 DTO
     */
    @Override
    public WarningEventDTO getById(Long warningId) {
        WarningEvent event = warningEventMapper.selectById(warningId);
        if (event == null) {
            throw new BizException(ErrorCode.WARNING_NOT_FOUND);
        }
        return toDto(event);
    }

    /**
     * 查询指定管网的活跃预警（待确认/已确认/处置中）。
     *
     * @param pipeId 管网ID
     * @return 活跃预警列表
     */
    @Override
    public List<WarningEventDTO> listActive(Long pipeId) {
        LambdaQueryWrapper<WarningEvent> wrapper = new LambdaQueryWrapper<WarningEvent>()
                .eq(WarningEvent::getPipeId, pipeId)
                .in(WarningEvent::getStatus, WarningStatus.PENDING, WarningStatus.CONFIRMED, WarningStatus.PROCESSING)
                .orderByDesc(WarningEvent::getCreatedAt);
        return warningEventMapper.selectList(wrapper).stream().map(this::toDto).toList();
    }

    /**
     * 查询指定区域的活跃预警（待确认/已确认/处置中，按 area_id 过滤）。
     *
     * @param areaId 区域ID
     * @return 活跃预警列表
     */
    @Override
    public List<WarningEventDTO> listActiveByArea(Long areaId) {
        LambdaQueryWrapper<WarningEvent> wrapper = new LambdaQueryWrapper<WarningEvent>()
                .eq(WarningEvent::getAreaId, areaId)
                .in(WarningEvent::getStatus, WarningStatus.PENDING, WarningStatus.CONFIRMED, WarningStatus.PROCESSING)
                .orderByDesc(WarningEvent::getCreatedAt);
        return warningEventMapper.selectList(wrapper).stream().map(this::toDto).toList();
    }

    /**
     * 创建预警事件（REST 入口，走统一幂等落库逻辑）。
     *
     * @param dto 预警事件 DTO
     * @return 创建后的预警事件（含ID）
     */
    @Override
    public WarningEventDTO create(WarningEventDTO dto) {
        if (dto == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "预警事件不能为空");
        }
        Integer level = parseLevel(dto.getWarningLevel());
        if (level == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "预警等级非法，取值 RED/ORANGE/YELLOW/BLUE 或 1-4");
        }
        String warningNo = dto.getWarningNo();
        if (warningNo == null || warningNo.isBlank()) {
            warningNo = "WM-" + SnowflakeIdGenerator.getInstance().nextIdStr();
        }

        // 第一重幂等：warningNo 级 Redis SETNX 抢占
        Duration ttl = Duration.ofMinutes(warningProperties.getIdempotentTtlMinutes());
        Boolean firstSeen = stringRedisTemplate.opsForValue()
                .setIfAbsent(WarningRedisKeys.noIdempotent(warningNo), "1", ttl);
        if (!Boolean.TRUE.equals(firstSeen)) {
            // 重复创建：回查已存在记录，保持幂等语义
            WarningEvent existing = findByEventCode(warningNo);
            if (existing != null) {
                return toDto(existing);
            }
            throw new BizException(ErrorCode.DUPLICATE_REQUEST, "预警编号已存在：" + warningNo);
        }

        WarningEvent entity = new WarningEvent();
        entity.setEventCode(warningNo);
        entity.setSource(WarningSource.normalize(dto.getSource()));
        entity.setWarningLevel(level);
        entity.setPipeType(dto.getPipeType());
        entity.setPipeId(dto.getPipeId());
        entity.setDeviceId(dto.getDeviceId());
        entity.setAreaId(dto.getAreaId());
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setMetricType(dto.getMetricType());
        entity.setWarningType(dto.getMetricType());
        entity.setTriggerValue(dto.getTriggerValue() == null ? null : BigDecimal.valueOf(dto.getTriggerValue()));
        entity.setThreshold(dto.getThreshold() == null ? null : BigDecimal.valueOf(dto.getThreshold()));
        entity.setAssigneeId(dto.getAssigneeId());
        entity.setStatus(WarningStatus.PENDING);

        try {
            warningEventMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            // 唯一索引兜底：并发下同一编号已落库，回查返回保持幂等
            WarningEvent existing = findByEventCode(warningNo);
            if (existing != null) {
                return toDto(existing);
            }
            throw new BizException(ErrorCode.DUPLICATE_REQUEST, "预警编号已存在：" + warningNo);
        }
        log.info("预警事件创建成功(REST): warningNo={}, level={}, source={}",
                warningNo, level, entity.getSource());
        // 异步 AI 深度研判回填（不阻塞 REST 主流程，失败降级不回填）
        aiJudgeBackfillService.submitAsyncJudge(entity);
        // 按等级生成推送并经 Kafka 异步落库留痕
        warningPushService.publishPushes(entity);
        return toDto(entity);
    }

    /**
     * 创建预警事件（Kafka 入口，幂等落库）。
     *
     * @param payload 预警事件负载
     * @param aiScore AI 异常评分，纯规则判定传 null
     * @return 落库后的实体；warningNo 重复时返回 null
     */
    @Override
    public WarningEvent createFromPayload(WarningEventPayload payload, BigDecimal aiScore) {
        String warningNo = payload.getWarningNo();
        if (warningNo == null || warningNo.isBlank()) {
            warningNo = "WK-" + SnowflakeIdGenerator.getInstance().nextIdStr();
        }
        // 第二重幂等：warningNo 级 Redis SETNX 抢占（第一重 eventId 去重在监听器）
        Duration ttl = Duration.ofMinutes(warningProperties.getIdempotentTtlMinutes());
        Boolean firstSeen = stringRedisTemplate.opsForValue()
                .setIfAbsent(WarningRedisKeys.noIdempotent(warningNo), "1", ttl);
        if (!Boolean.TRUE.equals(firstSeen)) {
            log.debug("预警编号重复已拦截: warningNo={}", warningNo);
            return null;
        }

        WarningLevel level = WarningLevel.fromCode(payload.getWarningLevel());
        WarningEvent entity = new WarningEvent();
        entity.setEventCode(warningNo);
        entity.setSource(WarningSource.normalize(payload.getSource()));
        entity.setWarningLevel(level == null ? WarningLevel.BLUE.getLevel() : level.getLevel());
        entity.setPipeType(payload.getPipeType());
        entity.setPipeId(payload.getPipeId());
        entity.setDeviceId(payload.getDeviceId());
        entity.setAreaId(payload.getAreaId());
        entity.setTitle(payload.getTitle());
        entity.setDescription(payload.getDescription());
        entity.setMetricType(payload.getMetricType());
        entity.setWarningType(payload.getWarningType());
        entity.setTriggerValue(payload.getTriggerValue() == null
                ? null : BigDecimal.valueOf(payload.getTriggerValue()));
        entity.setThreshold(payload.getThreshold() == null
                ? null : BigDecimal.valueOf(payload.getThreshold()));
        entity.setAssigneeId(payload.getAssigneeId());
        entity.setAiScore(aiScore);
        entity.setStatus(WarningStatus.PENDING);

        try {
            warningEventMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            // 唯一索引兜底：并发竞争下重复，直接忽略
            log.debug("预警事件唯一索引去重生效: warningNo={}", warningNo);
            return null;
        }
        log.info("预警事件落库成功(Kafka): warningNo={}, level={}, source={}, aiScore={}",
                warningNo, entity.getWarningLevel(), entity.getSource(), aiScore);
        return entity;
    }

    /**
     * 确认预警（状态机：0 待确认 → 1 已确认）。
     *
     * @param warningId 预警事件ID
     * @return 更新后的预警事件 DTO
     */
    @Override
    public WarningEventDTO confirm(Long warningId) {
        WarningEvent event = warningEventMapper.selectById(warningId);
        if (event == null) {
            throw new BizException(ErrorCode.WARNING_NOT_FOUND);
        }
        if (event.getStatus() == null || event.getStatus() != WarningStatus.PENDING) {
            throw new BizException(ErrorCode.WARNING_STATUS_INVALID,
                    "仅待确认状态的预警可确认，当前状态：" + WarningStatus.toText(event.getStatus()));
        }
        event.setStatus(WarningStatus.CONFIRMED);
        event.setConfirmedBy(SecurityContext.getUserId());
        event.setConfirmedAt(LocalDateTime.now());
        warningEventMapper.updateById(event);
        log.info("预警事件已确认: id={}, warningNo={}, confirmedBy={}",
                warningId, event.getEventCode(), event.getConfirmedBy());
        return toDto(event);
    }

    /**
     * 核销预警（状态机：0/1/2 → 3 已核销，写 resolved_at）。
     *
     * @param warningId 预警事件ID
     * @return 更新后的预警事件 DTO
     */
    @Override
    public WarningEventDTO cancel(Long warningId) {
        WarningEvent event = warningEventMapper.selectById(warningId);
        if (event == null) {
            throw new BizException(ErrorCode.WARNING_NOT_FOUND);
        }
        Integer status = event.getStatus();
        boolean cancellable = status != null
                && (status == WarningStatus.PENDING || status == WarningStatus.CONFIRMED
                || status == WarningStatus.PROCESSING);
        if (!cancellable) {
            throw new BizException(ErrorCode.WARNING_STATUS_INVALID,
                    "当前状态不允许核销：" + WarningStatus.toText(status));
        }
        event.setStatus(WarningStatus.RESOLVED);
        event.setResolvedAt(LocalDateTime.now());
        warningEventMapper.updateById(event);
        log.info("预警事件已核销: id={}, warningNo={}", warningId, event.getEventCode());
        return toDto(event);
    }

    /**
     * 按等级/管网类型/状态三维统计预警事件。
     *
     * @return 统计结果
     */
    @Override
    public WarningStatisticsVO statistics() {
        WarningStatisticsVO vo = new WarningStatisticsVO();
        vo.setTotal(warningEventMapper.selectCount(null));

        List<WarningStatisticsVO.GroupItem> byLevel = new ArrayList<>();
        for (Map<String, Object> row : warningEventMapper.countGroupByLevel()) {
            Integer levelValue = toInt(row.get("groupKey"));
            WarningLevel level = levelValue == null ? null : WarningLevel.fromLevel(levelValue);
            String key = level == null ? "UNKNOWN" : level.getCode();
            String label = level == null ? "未知等级" : level.getDescription();
            byLevel.add(new WarningStatisticsVO.GroupItem(key, label, toLong(row.get("cnt"))));
        }
        vo.setByLevel(byLevel);

        List<WarningStatisticsVO.GroupItem> byType = new ArrayList<>();
        for (Map<String, Object> row : warningEventMapper.countGroupByPipeType()) {
            Object key = row.get("groupKey");
            String type = key == null ? "UNKNOWN" : String.valueOf(key);
            byType.add(new WarningStatisticsVO.GroupItem(type, type, toLong(row.get("cnt"))));
        }
        vo.setByType(byType);

        List<WarningStatisticsVO.GroupItem> byStatus = new ArrayList<>();
        for (Map<String, Object> row : warningEventMapper.countGroupByStatus()) {
            Integer statusValue = toInt(row.get("groupKey"));
            String text = WarningStatus.toText(statusValue);
            byStatus.add(new WarningStatisticsVO.GroupItem(
                    statusValue == null ? "UNKNOWN" : String.valueOf(statusValue),
                    text.isEmpty() ? "未知状态" : text,
                    toLong(row.get("cnt"))));
        }
        vo.setByStatus(byStatus);
        return vo;
    }

    /**
     * 按事件编号查询未删除记录。
     *
     * @param eventCode 事件编号
     * @return 预警事件，不存在返回 null
     */
    private WarningEvent findByEventCode(String eventCode) {
        return warningEventMapper.selectOne(new LambdaQueryWrapper<WarningEvent>()
                .eq(WarningEvent::getEventCode, eventCode)
                .last("LIMIT 1"));
    }

    /**
     * 解析预警等级参数（兼容编码 RED/ORANGE/YELLOW/BLUE 与数值 1-4）。
     *
     * @param value 等级参数
     * @return 级别数值，非法返回 null
     */
    private Integer parseLevel(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        WarningLevel level = WarningLevel.fromCode(value.trim());
        if (level != null) {
            return level.getLevel();
        }
        try {
            int num = Integer.parseInt(value.trim());
            return WarningLevel.fromLevel(num) == null ? null : num;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 解析预警状态参数（兼容中文与数值 0-4）。
     *
     * @param value 状态参数
     * @return 状态数值，非法返回 null
     */
    private Integer parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        Integer code = WarningStatus.fromText(value.trim());
        if (code != null) {
            return code;
        }
        try {
            int num = Integer.parseInt(value.trim());
            return num >= 0 && num <= 4 ? num : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 解析时间过滤参数（yyyy-MM-dd HH:mm:ss），非法返回 null。
     *
     * @param value 时间字符串
     * @return 时间对象
     */
    private LocalDateTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim(), TIME_FORMATTER);
        } catch (Exception e) {
            log.warn("时间参数格式非法已忽略: {}", value);
            return null;
        }
    }

    /**
     * Map 取值转 Integer。
     *
     * @param value Map 值
     * @return Integer，非数值返回 null
     */
    private Integer toInt(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    /**
     * Map 取值转 Long。
     *
     * @param value Map 值
     * @return Long，非数值返回 0
     */
    private Long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    /**
     * 实体转契约 DTO（状态输出中文，等级输出编码）。
     *
     * @param event 预警事件实体
     * @return 预警事件 DTO
     */
    private WarningEventDTO toDto(WarningEvent event) {
        WarningEventDTO dto = new WarningEventDTO();
        dto.setWarningId(event.getId());
        dto.setWarningNo(event.getEventCode());
        WarningLevel level = event.getWarningLevel() == null
                ? null : WarningLevel.fromLevel(event.getWarningLevel());
        dto.setWarningLevel(level == null ? null : level.getCode());
        dto.setSource(event.getSource());
        dto.setPipeType(event.getPipeType());
        dto.setPipeId(event.getPipeId());
        dto.setDeviceId(event.getDeviceId());
        dto.setAreaId(event.getAreaId());
        dto.setTitle(event.getTitle());
        dto.setDescription(event.getDescription());
        dto.setMetricType(event.getMetricType());
        dto.setTriggerValue(event.getTriggerValue() == null ? null : event.getTriggerValue().doubleValue());
        dto.setThreshold(event.getThreshold() == null ? null : event.getThreshold().doubleValue());
        dto.setStatus(WarningStatus.toText(event.getStatus()));
        dto.setWarningTime(event.getCreatedAt());
        dto.setAssigneeId(event.getAssigneeId());
        return dto;
    }

    /**
     * 实体转视图对象（附加等级名称/色值与中文状态）。
     *
     * @param event 预警事件实体
     * @return 预警事件视图对象
     */
    private WarningEventVO toVo(WarningEvent event) {
        WarningEventVO vo = new WarningEventVO();
        vo.setWarningId(event.getId());
        vo.setWarningNo(event.getEventCode());
        WarningLevel level = event.getWarningLevel() == null
                ? null : WarningLevel.fromLevel(event.getWarningLevel());
        vo.setWarningLevel(level == null ? null : level.getCode());
        vo.setLevelName(level == null ? null : level.getDescription());
        vo.setLevelColor(level == null ? null : level.getColor());
        vo.setSource(event.getSource());
        vo.setPipeType(event.getPipeType());
        vo.setPipeId(event.getPipeId());
        vo.setDeviceId(event.getDeviceId());
        vo.setAreaId(event.getAreaId());
        vo.setTitle(event.getTitle());
        vo.setDescription(event.getDescription());
        vo.setMetricType(event.getMetricType());
        vo.setTriggerValue(event.getTriggerValue());
        vo.setThreshold(event.getThreshold());
        vo.setAiScore(event.getAiScore());
        vo.setAiResult(event.getAiResult());
        vo.setAiJudgedAt(event.getAiJudgedAt());
        vo.setStatus(WarningStatus.toText(event.getStatus()));
        vo.setWarningTime(event.getCreatedAt());
        vo.setConfirmedAt(event.getConfirmedAt());
        vo.setResolvedAt(event.getResolvedAt());
        vo.setAssigneeId(event.getAssigneeId());
        return vo;
    }
}
