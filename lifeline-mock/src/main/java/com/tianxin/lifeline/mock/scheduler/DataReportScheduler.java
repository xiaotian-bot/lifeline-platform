package com.tianxin.lifeline.mock.scheduler;

import com.tianxin.lifeline.common.core.constant.PipeType;
import com.tianxin.lifeline.mock.config.MockProperties;
import com.tianxin.lifeline.mock.generator.SensorDataGenerator;
import com.tianxin.lifeline.mock.model.DeviceInfo;
import com.tianxin.lifeline.mock.model.SensorData;
import com.tianxin.lifeline.mock.scenario.AnomalyScenario;
import com.tianxin.lifeline.mock.scenario.ScenarioManager;
import com.tianxin.lifeline.mock.sender.KafkaDataSender;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 定时上报调度器。
 *
 * <p>按配置周期驱动全部模拟设备生成传感数据并上报 Kafka：
 * <ol>
 *     <li>遍历设备清单，按管网类型路由到对应生成器（策略模式）；</li>
 *     <li>叠加场景管理器中激活的异常效果；</li>
 *     <li>按 {@code mock.anomaly.auto-trigger} 概率自动注入新异常；</li>
 *     <li>整批交由 {@link KafkaDataSender} 上报。</li>
 * </ol>
 *
 * <p>支持运行时启停与动态调整设备数、上报周期（REST 控制台驱动）。
 */
@Slf4j
@Component
public class DataReportScheduler {

    /** 种子设备的主传感器类型（与 03-seed-data.sql 对齐） */
    private static final Map<String, String> SEED_SENSOR_TYPES = Map.ofEntries(
            Map.entry("DEV-GAS-001", "pressure"), Map.entry("DEV-GAS-002", "leakage"),
            Map.entry("DEV-WAT-001", "pressure"), Map.entry("DEV-WAT-002", "flow"),
            Map.entry("DEV-DRN-001", "flow"), Map.entry("DEV-DRN-002", "leakage"),
            Map.entry("DEV-HTG-001", "temperature"), Map.entry("DEV-HTG-002", "pressure"),
            Map.entry("DEV-STR-001", "flow"), Map.entry("DEV-STR-002", "vibration"));

    /** 各管网类型的轮询传感器类型（非种子设备使用） */
    private static final Map<PipeType, List<String>> PIPE_SENSOR_TYPES = Map.of(
            PipeType.WATER, List.of("pressure", "flow", "temperature"),
            PipeType.GAS, List.of("pressure", "leakage", "flow"),
            PipeType.SEWAGE, List.of("flow", "leakage", "temperature"),
            PipeType.HEAT, List.of("temperature", "pressure", "flow"),
            PipeType.HAZARD, List.of("corrosion", "pressure", "vibration"));

    /** Mock 配置 */
    private final MockProperties properties;

    /** Kafka 发送器 */
    private final KafkaDataSender sender;

    /** 异常场景管理器 */
    private final ScenarioManager scenarioManager;

    /** 管网类型 -> 生成器 */
    private final Map<PipeType, SensorDataGenerator> generatorMap = new EnumMap<>(PipeType.class);

    /** 模拟设备清单 */
    private volatile List<DeviceInfo> devices = List.of();

    /** 调度线程池 */
    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "mock-report-scheduler");
                t.setDaemon(true);
                return t;
            });

    /** 当前调度任务句柄 */
    private volatile ScheduledFuture<?> scheduledFuture;

    /** 运行状态标记 */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /** 累计上报数据条数 */
    private final AtomicLong totalRecords = new AtomicLong(0);

    /** 累计上报轮次 */
    private final AtomicLong reportCycles = new AtomicLong(0);

    /** 最近一轮上报条数 */
    private volatile int lastBatchSize = 0;

    /** 最近一轮上报时间戳 */
    private volatile long lastReportTs = 0;

    /**
     * 构造调度器，注册全部管网生成器。
     *
     * @param properties      Mock 配置
     * @param sender          Kafka 发送器
     * @param scenarioManager 异常场景管理器
     * @param generators      全部传感数据生成器（Spring 注入）
     */
    public DataReportScheduler(MockProperties properties,
                               KafkaDataSender sender,
                               ScenarioManager scenarioManager,
                               List<SensorDataGenerator> generators) {
        this.properties = properties;
        this.sender = sender;
        this.scenarioManager = scenarioManager;
        for (SensorDataGenerator generator : generators) {
            generatorMap.put(generator.supportedPipeType(), generator);
        }
    }

    /**
     * 启动时构建设备清单，若配置启用则自动开始上报。
     */
    @PostConstruct
    public void init() {
        rebuildDevices();
        if (properties.isEnabled()) {
            start();
        } else {
            log.info("Mock数据生成未启用(mock.enabled=false), 等待手动启动");
        }
    }

    /**
     * 启动数据上报调度。
     *
     * @return true 表示本次调用成功启动
     */
    public synchronized boolean start() {
        if (running.get()) {
            return false;
        }
        int interval = properties.getSensor().getReportIntervalSeconds();
        scheduledFuture = executor.scheduleWithFixedDelay(this::reportCycle, 0, interval, TimeUnit.SECONDS);
        running.set(true);
        log.info("Mock数据上报已启动: 设备数={}, 周期={}秒, 压测模式={}",
                devices.size(), interval, properties.getStress().isMode());
        return true;
    }

    /**
     * 停止数据上报调度。
     *
     * @return true 表示本次调用成功停止
     */
    public synchronized boolean stop() {
        if (!running.get()) {
            return false;
        }
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            scheduledFuture = null;
        }
        running.set(false);
        log.info("Mock数据上报已停止");
        return true;
    }

    /**
     * 动态调整配置：设备数量与上报周期。
     *
     * @param deviceCount      新设备数（null 表示不变）
     * @param intervalSeconds  新上报周期秒数（null 表示不变）
     */
    public synchronized void updateConfig(Integer deviceCount, Integer intervalSeconds) {
        boolean rebuild = false;
        if (deviceCount != null && deviceCount > 0) {
            properties.getSensor().setCount(deviceCount);
            rebuild = true;
        }
        if (intervalSeconds != null && intervalSeconds > 0) {
            properties.getSensor().setReportIntervalSeconds(intervalSeconds);
        }
        if (rebuild) {
            rebuildDevices();
        }
        // 周期或设备数变化后重新调度，保证立即生效
        if (running.get()) {
            stop();
            start();
        }
        log.info("Mock配置已更新: 设备数={}, 周期={}秒",
                devices.size(), properties.getSensor().getReportIntervalSeconds());
    }

    /**
     * 单轮上报：生成 -> 叠加异常 -> 自动注入 -> 发送。
     */
    private void reportCycle() {
        try {
            long ts = System.currentTimeMillis();
            List<DeviceInfo> snapshot = devices;
            List<SensorData> batch = new ArrayList<>(snapshot.size());
            for (DeviceInfo device : snapshot) {
                SensorDataGenerator generator = generatorMap.get(device.getPipeType());
                if (generator == null) {
                    continue;
                }
                SensorData data = generator.generate(device, ts);
                scenarioManager.applyIfActive(data);
                batch.add(data);
            }
            autoTriggerAnomaly(snapshot);
            int sent = sender.send(batch);
            totalRecords.addAndGet(sent);
            reportCycles.incrementAndGet();
            lastBatchSize = sent;
            lastReportTs = System.currentTimeMillis();
            log.debug("Mock数据上报完成: 本轮{}条, 累计{}条", sent, totalRecords.get());
        } catch (Exception e) {
            log.error("Mock数据上报轮次执行失败", e);
        }
    }

    /**
     * 自动异常触发：开启 auto-trigger 时每轮以 5% 概率随机注入一个异常。
     *
     * @param snapshot 当前设备清单
     */
    private void autoTriggerAnomaly(List<DeviceInfo> snapshot) {
        if (!properties.getAnomaly().isAutoTrigger() || snapshot.isEmpty()) {
            return;
        }
        if (ThreadLocalRandom.current().nextDouble() >= 0.05) {
            return;
        }
        List<String> types = properties.getAnomaly().getTypes();
        if (types == null || types.isEmpty()) {
            return;
        }
        AnomalyScenario scenario = AnomalyScenario.fromCode(
                types.get(ThreadLocalRandom.current().nextInt(types.size())));
        if (scenario == null) {
            return;
        }
        DeviceInfo victim = snapshot.get(ThreadLocalRandom.current().nextInt(snapshot.size()));
        if (!scenarioManager.isActive(victim.getDeviceId())) {
            scenarioManager.trigger(victim.getDeviceId(), scenario);
        }
    }

    /**
     * 构建设备清单。
     *
     * <p>压测模式下按 {@code mock.stress.device-count} 分档生成；
     * 常规模式按 {@code mock.sensor.count} 生成，前 10 台为种子设备。
     */
    private void rebuildDevices() {
        List<String> typeCodes = properties.getSensor().getPipeTypes();
        int count = properties.getStress().isMode()
                ? properties.getStress().getDeviceCount()
                : properties.getSensor().getCount();
        Map<String, DeviceInfo> map = new ConcurrentHashMap<>();
        Map<String, Integer> seqCounter = new ConcurrentHashMap<>();
        List<DeviceInfo> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String typeCode = typeCodes.get(i % typeCodes.size());
            int seq = seqCounter.merge(typeCode, 1, Integer::sum);
            String deviceId = DeviceInfo.buildDeviceId(typeCode, seq);
            PipeType pipeType = DeviceInfo.CODE_TO_PIPE_TYPE.get(typeCode);
            if (pipeType == null) {
                continue;
            }
            boolean seed = SEED_SENSOR_TYPES.containsKey(deviceId);
            String sensorType = seed
                    ? SEED_SENSOR_TYPES.get(deviceId)
                    : pickSensorType(pipeType, seq);
            DeviceInfo device = DeviceInfo.builder()
                    .deviceId(deviceId)
                    .pipeType(pipeType)
                    .sensorType(sensorType)
                    .areaId(String.format("AREA-%03d", (i % 5) + 1))
                    .seed(seed)
                    .build();
            map.put(deviceId, device);
            list.add(device);
        }
        this.devices = List.copyOf(list);
        log.info("设备清单已构建: 总数={}, 种子设备数={}", list.size(),
                list.stream().filter(DeviceInfo::isSeed).count());
    }

    /**
     * 为非种子设备轮询分配传感器类型。
     *
     * @param pipeType 管网类型
     * @param seq      设备序号
     * @return 传感器类型
     */
    private String pickSensorType(PipeType pipeType, int seq) {
        List<String> types = PIPE_SENSOR_TYPES.get(pipeType);
        if (types == null || types.isEmpty()) {
            return "pressure";
        }
        return types.get((seq - 1) % types.size());
    }

    /**
     * 查询调度器运行状态快照。
     *
     * @return 状态信息（有序 Map）
     */
    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("running", running.get());
        status.put("deviceCount", devices.size());
        status.put("reportIntervalSeconds", properties.getSensor().getReportIntervalSeconds());
        status.put("stressMode", properties.getStress().isMode());
        status.put("stressDeviceCount", properties.getStress().getDeviceCount());
        status.put("autoTriggerAnomaly", properties.getAnomaly().isAutoTrigger());
        status.put("activeAnomalyCount", scenarioManager.activeCount());
        status.put("totalRecordsSent", totalRecords.get());
        status.put("reportCycles", reportCycles.get());
        status.put("lastBatchSize", lastBatchSize);
        status.put("lastReportTs", lastReportTs);
        return status;
    }

    /**
     * 应用关闭时释放调度线程池。
     */
    @PreDestroy
    public void shutdown() {
        stop();
        executor.shutdown();
    }
}
