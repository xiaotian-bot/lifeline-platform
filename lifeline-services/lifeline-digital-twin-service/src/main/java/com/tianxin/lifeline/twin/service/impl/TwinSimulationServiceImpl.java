package com.tianxin.lifeline.twin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianxin.lifeline.common.core.exception.BizException;
import com.tianxin.lifeline.common.core.id.SnowflakeIdGenerator;
import com.tianxin.lifeline.common.core.result.ErrorCode;
import com.tianxin.lifeline.twin.config.TwinProperties;
import com.tianxin.lifeline.twin.constant.SimulationStatus;
import com.tianxin.lifeline.twin.constant.TwinRedisKeys;
import com.tianxin.lifeline.twin.dto.SimulationOutcome;
import com.tianxin.lifeline.twin.dto.SimulationRequest;
import com.tianxin.lifeline.twin.dto.SimulationResultVO;
import com.tianxin.lifeline.twin.dto.SimulationSubmitVO;
import com.tianxin.lifeline.twin.service.TwinSimulationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * 仿真推演服务实现。
 *
 * <p>推演模型为基于规则的轻量场景影响评估（非数值仿真）：
 * 场景基础影响半径 × 管线类型系数 × 预警等级系数 → 影响范围与等级，
 * 并按场景输出建议隔离区段与处置建议。
 *
 * <p>任务生命周期：提交即写 RUNNING 状态 HASH（TTL 30 分钟）并返回 taskId，
 * 异步线程池执行推演后回写 COMPLETED/FAILED；全程不落 PG。
 */
@Slf4j
@Service
public class TwinSimulationServiceImpl implements TwinSimulationService {

    /* ==================== 场景类型与基础影响半径（米） ==================== */

    /** 场景：泄漏 */
    private static final String SCENARIO_LEAKAGE = "LEAKAGE";

    /** 场景：爆管 */
    private static final String SCENARIO_BURST = "BURST";

    /** 场景：压力异常 */
    private static final String SCENARIO_PRESSURE_ABNORMAL = "PRESSURE_ABNORMAL";

    /** 场景：第三方破坏 */
    private static final String SCENARIO_THIRD_PARTY_DAMAGE = "THIRD_PARTY_DAMAGE";

    /** 默认场景基础影响半径（米） */
    private static final double DEFAULT_BASE_RADIUS = 100.0;

    /* ==================== 影响等级 ==================== */

    /** 影响等级：低 */
    private static final String IMPACT_LOW = "LOW";

    /** 影响等级：中 */
    private static final String IMPACT_MEDIUM = "MEDIUM";

    /** 影响等级：高 */
    private static final String IMPACT_HIGH = "HIGH";

    /** 影响等级：严重 */
    private static final String IMPACT_CRITICAL = "CRITICAL";

    /** 预警等级：红色（触发影响等级抬升） */
    private static final String LEVEL_RED = "RED";

    /** 任务 TTL 默认值（分钟，配置非法时回退） */
    private static final long DEFAULT_TTL_MINUTES = 30L;

    /** Redis 字符串模板（任务状态承载） */
    private final StringRedisTemplate stringRedisTemplate;

    /** JSON 序列化器（推演结果存取） */
    private final ObjectMapper objectMapper;

    /** 仿真推演异步线程池 */
    private final Executor simulationExecutor;

    /** 业务参数 */
    private final TwinProperties properties;

    /**
     * 构造服务。
     *
     * @param stringRedisTemplate Redis 字符串模板
     * @param objectMapper        JSON 序列化器
     * @param simulationExecutor  仿真推演线程池
     * @param properties          业务参数
     */
    public TwinSimulationServiceImpl(StringRedisTemplate stringRedisTemplate,
                                     ObjectMapper objectMapper,
                                     @Qualifier("twinSimulationExecutor") Executor simulationExecutor,
                                     TwinProperties properties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.simulationExecutor = simulationExecutor;
        this.properties = properties;
    }

    /**
     * {@inheritDoc}
     *
     * <p>提交动作只写初始状态并异步触发推演，请求快速返回不阻塞。
     */
    @Override
    public SimulationSubmitVO submit(SimulationRequest request) {
        String taskId = String.valueOf(SnowflakeIdGenerator.getInstance().nextId());
        long submitTime = System.currentTimeMillis();

        Map<String, String> fields = new HashMap<>(8);
        fields.put(SimulationStatus.FIELD_TASK_ID, taskId);
        fields.put(SimulationStatus.FIELD_STATUS, SimulationStatus.RUNNING);
        fields.put(SimulationStatus.FIELD_SCENARIO, request.getScenarioType());
        fields.put(SimulationStatus.FIELD_SUBMIT_TIME, String.valueOf(submitTime));
        String key = TwinRedisKeys.simulationKey(taskId);
        try {
            stringRedisTemplate.opsForHash().putAll(key, fields);
        } catch (Exception e) {
            log.error("仿真任务初始状态写入Redis失败: taskId={}", taskId, e);
            throw new BizException(ErrorCode.REMOTE_CALL_ERROR, "仿真任务创建失败，请稍后重试");
        }
        // expire 失败不阻断提交（键仍存活但不过期），单独告警
        refreshExpire(key, taskId);

        // 异步执行规则推演，禁止长请求阻塞
        simulationExecutor.execute(() -> runSimulation(taskId, key, request));

        SimulationSubmitVO vo = new SimulationSubmitVO();
        vo.setTaskId(taskId);
        vo.setStatus(SimulationStatus.RUNNING);
        vo.setSubmitTime(submitTime);
        return vo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SimulationResultVO query(String taskId) {
        Map<Object, Object> entries;
        try {
            entries = stringRedisTemplate.opsForHash().entries(TwinRedisKeys.simulationKey(taskId));
        } catch (Exception e) {
            log.error("仿真任务状态读取Redis失败: taskId={}", taskId, e);
            throw new BizException(ErrorCode.REMOTE_CALL_ERROR, "仿真任务状态查询失败");
        }
        if (entries == null || entries.isEmpty()) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "仿真任务不存在或已过期");
        }

        SimulationResultVO vo = new SimulationResultVO();
        vo.setTaskId(taskId);
        vo.setStatus(field(entries, SimulationStatus.FIELD_STATUS));
        vo.setScenarioType(field(entries, SimulationStatus.FIELD_SCENARIO));
        vo.setSubmitTime(asLong(field(entries, SimulationStatus.FIELD_SUBMIT_TIME)));
        vo.setFinishTime(asLong(field(entries, SimulationStatus.FIELD_FINISH_TIME)));
        vo.setError(field(entries, SimulationStatus.FIELD_ERROR));
        String resultJson = field(entries, SimulationStatus.FIELD_RESULT);
        if (resultJson != null && !resultJson.isBlank()) {
            try {
                vo.setOutcome(objectMapper.readValue(resultJson, SimulationOutcome.class));
            } catch (Exception e) {
                log.warn("仿真结果JSON反序列化失败: taskId={}", taskId, e);
            }
        }
        return vo;
    }

    /**
     * 异步执行规则推演并回写任务状态（异常仅影响任务状态，不影响接口层）。
     *
     * @param taskId  任务ID
     * @param key     Redis 键
     * @param request 推演请求
     */
    private void runSimulation(String taskId, String key, SimulationRequest request) {
        Map<String, String> updates = new HashMap<>(4);
        try {
            SimulationOutcome outcome = evaluate(request);
            updates.put(SimulationStatus.FIELD_STATUS, SimulationStatus.COMPLETED);
            updates.put(SimulationStatus.FIELD_RESULT, objectMapper.writeValueAsString(outcome));
        } catch (Exception e) {
            log.error("仿真推演执行失败: taskId={}", taskId, e);
            updates.put(SimulationStatus.FIELD_STATUS, SimulationStatus.FAILED);
            updates.put(SimulationStatus.FIELD_ERROR, e.getMessage() == null ? "推演异常" : e.getMessage());
        }
        updates.put(SimulationStatus.FIELD_FINISH_TIME, String.valueOf(System.currentTimeMillis()));
        try {
            stringRedisTemplate.opsForHash().putAll(key, updates);
        } catch (Exception e) {
            log.error("仿真任务结果回写Redis失败: taskId={}", taskId, e);
        }
        refreshExpire(key, taskId);
    }

    /**
     * 刷新任务键 TTL（失败仅告警不中断：键仍存活但不过期，需人工关注）。
     *
     * @param key    Redis 键
     * @param taskId 任务ID（日志定位用）
     */
    private void refreshExpire(String key, String taskId) {
        try {
            stringRedisTemplate.expire(key, Duration.ofMinutes(resolveTtlMinutes()));
        } catch (Exception e) {
            log.warn("仿真任务TTL设置失败, 键将不过期需人工关注: taskId={}", taskId, e);
        }
    }

    /**
     * 解析任务 TTL 配置，非法值（≤0）防御回退默认 30 分钟。
     *
     * @return 有效的 TTL 分钟数
     */
    private long resolveTtlMinutes() {
        long configured = properties.getSimulationTtlMinutes();
        if (configured <= 0) {
            log.warn("lifeline.twin.simulation-ttl-minutes 配置非法({}), 回退默认值 30 分钟", configured);
            return DEFAULT_TTL_MINUTES;
        }
        return configured;
    }

    /**
     * 规则推演核心：场景基础半径 × 管线类型系数 × 预警等级系数，
     * 输出影响范围、隔离区段与处置建议。
     *
     * @param request 推演请求
     * @return 结构化推演结果
     */
    private SimulationOutcome evaluate(SimulationRequest request) {
        String scenario = request.getScenarioType().trim().toUpperCase();
        double baseRadius = switch (scenario) {
            case SCENARIO_BURST -> 300.0;
            case SCENARIO_PRESSURE_ABNORMAL -> 50.0;
            case SCENARIO_THIRD_PARTY_DAMAGE -> 80.0;
            case SCENARIO_LEAKAGE -> 100.0;
            default -> DEFAULT_BASE_RADIUS;
        };
        double pipeFactor = switch (request.getPipeType() == null ? "" : request.getPipeType().toUpperCase()) {
            case "GAS" -> 1.5;
            case "HAZARD" -> 2.0;
            case "HEAT" -> 1.2;
            default -> 1.0;
        };
        double levelFactor = switch (request.getWarningLevel() == null ? "" : request.getWarningLevel().toUpperCase()) {
            case "YELLOW" -> 1.2;
            case "ORANGE" -> 1.5;
            case LEVEL_RED -> 2.0;
            default -> 1.0;
        };
        double radius = Math.round(baseRadius * pipeFactor * levelFactor);

        SimulationOutcome outcome = new SimulationOutcome();
        outcome.setScenarioType(scenario);
        outcome.setPipeType(request.getPipeType());
        outcome.setWarningLevel(request.getWarningLevel());
        outcome.setImpactRadiusMeters(radius);
        outcome.setImpactAreaSquareMeters(Math.round(Math.PI * radius * radius));
        outcome.setImpactLevel(resolveImpactLevel(radius, request.getWarningLevel()));
        outcome.setIsolationSegments(buildIsolationSegments(scenario, radius));
        outcome.setSuggestions(buildSuggestions(scenario, request.getPipeType()));
        outcome.setSummary(String.format(
                "场景 %s 推演完成：估算影响半径 %.0f 米、影响面积约 %.0f 平方米，影响等级 %s，建议按隔离区段清单执行应急处置。",
                scenario, radius, outcome.getImpactAreaSquareMeters(), outcome.getImpactLevel()));
        return outcome;
    }

    /**
     * 按影响半径与预警等级确定影响等级（红色预警至少为 HIGH）。
     *
     * @param radius       影响半径（米）
     * @param warningLevel 预警等级（可空）
     * @return 影响等级
     */
    private String resolveImpactLevel(double radius, String warningLevel) {
        String level;
        if (radius >= 400) {
            level = IMPACT_CRITICAL;
        } else if (radius >= 200) {
            level = IMPACT_HIGH;
        } else if (radius >= 100) {
            level = IMPACT_MEDIUM;
        } else {
            level = IMPACT_LOW;
        }
        if (LEVEL_RED.equalsIgnoreCase(warningLevel) && (IMPACT_LOW.equals(level) || IMPACT_MEDIUM.equals(level))) {
            level = IMPACT_HIGH;
        }
        return level;
    }

    /**
     * 生成建议隔离区段清单。
     *
     * @param scenario 场景类型
     * @param radius   影响半径（米）
     * @return 隔离区段描述列表
     */
    private List<String> buildIsolationSegments(String scenario, double radius) {
        List<String> segments = new ArrayList<>();
        segments.add(String.format("以故障点为圆心、半径 %.0f 米划定隔离区，设置警戒标识", radius));
        segments.add("关闭故障管段上下游最近控制阀门，形成物理隔离区段");
        if (SCENARIO_BURST.equals(scenario)) {
            segments.add(String.format("爆管场景追加隔离：故障点上下游各 %.0f 米管段停止运行", radius / 2));
        }
        if (SCENARIO_THIRD_PARTY_DAMAGE.equals(scenario)) {
            segments.add("第三方破坏场景追加隔离：保护现场并圈定施工机械作业半径为禁入区");
        }
        return segments;
    }

    /**
     * 生成处置建议清单（按场景与管线类型）。
     *
     * @param scenario 场景类型
     * @param pipeType 管线类型（可空）
     * @return 处置建议列表
     */
    private List<String> buildSuggestions(String scenario, String pipeType) {
        List<String> suggestions = new ArrayList<>();
        switch (scenario) {
            case SCENARIO_BURST -> suggestions.add("启动爆管应急预案，优先调度抢修队伍与备品备件");
            case SCENARIO_PRESSURE_ABNORMAL -> suggestions.add("复核压力监测数据并排查泵站/调压设施运行工况");
            case SCENARIO_THIRD_PARTY_DAMAGE -> suggestions.add("联动施工监护服务核实现场作业并制止破坏行为");
            default -> suggestions.add("启动泄漏应急预案，安排巡检人员现场确认泄漏点");
        }
        if ("GAS".equalsIgnoreCase(pipeType)) {
            suggestions.add("燃气管线：疏散下风向人员并禁止明火，通知燃气公司协同处置");
        } else if ("HAZARD".equalsIgnoreCase(pipeType)) {
            suggestions.add("危化品管线：通知应急管理部门，按危化品预案穿戴防护装备处置");
        } else if ("HEAT".equalsIgnoreCase(pipeType)) {
            suggestions.add("供热管线：注意高温水汽防烫，先降温泄压再抢修");
        }
        suggestions.add("处置完成后回写预警状态并归档推演结果");
        return suggestions;
    }

    /** HASH 字段取值（null 安全） */
    private String field(Map<Object, Object> entries, String field) {
        Object value = entries.get(field);
        return value == null ? null : String.valueOf(value);
    }

    /** 字符串转 Long（null 与格式异常安全） */
    private Long asLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
