package com.tianxin.lifeline.mock.scenario;

import com.tianxin.lifeline.mock.model.SensorData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 异常场景管理器。
 *
 * <p>负责异常场景的注入、演进与清除：
 * <ul>
 *     <li>通过 REST API 或自动定时器触发 {@link #trigger} 注入异常；</li>
 *     <li>每次数据生成后调用 {@link #applyIfActive} 将异常效果叠加到传感数据；</li>
 *     <li>场景到期自动清除，也可通过 {@link #clear} 手动清除。</li>
 * </ul>
 *
 * <p>场景效果采用策略模式：{@link ScenarioEffect} 为效果策略接口，
 * {@link PressureDropScenario}、{@link GasLeakScenario}、{@link HeatingLeakScenario}、
 * {@link DrainageOverflowScenario}、{@link ThirdPartyDamageScenario} 为独立实现，
 * TEMP_SPIKE 使用内置默认效果。
 */
@Slf4j
@Component
public class ScenarioManager {

    /**
     * 异常场景效果策略接口。
     */
    public interface ScenarioEffect {

        /**
         * 将异常效果叠加到传感数据上。
         *
         * @param data     原始传感数据（原地修改）
         * @param progress 场景演进进度，0~1
         */
        void apply(SensorData data, double progress);
    }

    /**
     * 激活中的异常场景记录。
     *
     * @param scenario  场景类型
     * @param startTime 注入时间戳（毫秒）
     * @param endTime   到期时间戳（毫秒）
     */
    public record ActiveScenario(AnomalyScenario scenario, long startTime, long endTime) {

        /**
         * 计算当前演进进度。
         *
         * @param now 当前时间戳
         * @return 进度 0~1
         */
        public double progress(long now) {
            long total = endTime - startTime;
            if (total <= 0) {
                return 1.0;
            }
            return Math.min(1.0, Math.max(0.0, (now - startTime) / (double) total));
        }

        /**
         * 判断场景是否已到期。
         *
         * @param now 当前时间戳
         * @return true 表示已到期
         */
        public boolean expired(long now) {
            return now >= endTime;
        }
    }

    /** 设备ID -> 激活中的异常场景 */
    private final Map<String, ActiveScenario> activeScenarios = new ConcurrentHashMap<>();

    /** 场景类型 -> 效果策略 */
    private final Map<AnomalyScenario, ScenarioEffect> effects = new ConcurrentHashMap<>();

    /**
     * 构造管理器并注册全部场景效果策略。
     *
     * @param pressureDropScenario     压降场景独立实现（由 Spring 注入）
     * @param gasLeakScenario          燃气泄漏场景独立实现（由 Spring 注入）
     * @param heatingLeakScenario      供热泄漏场景独立实现（由 Spring 注入）
     * @param drainageOverflowScenario 排水溢流场景独立实现（由 Spring 注入）
     * @param thirdPartyDamageScenario 第三方破坏场景独立实现（由 Spring 注入）
     */
    public ScenarioManager(PressureDropScenario pressureDropScenario,
                           GasLeakScenario gasLeakScenario,
                           HeatingLeakScenario heatingLeakScenario,
                           DrainageOverflowScenario drainageOverflowScenario,
                           ThirdPartyDamageScenario thirdPartyDamageScenario) {
        effects.put(AnomalyScenario.PRESSURE_DROP, pressureDropScenario);
        // 燃气泄漏：leakage 浓度陡升并伴随 pressure 下降
        effects.put(AnomalyScenario.GAS_LEAK, gasLeakScenario);
        // 供热泄漏：flow 骤降并伴随 temperature 异常跌落
        effects.put(AnomalyScenario.HEATING_LEAK, heatingLeakScenario);
        // 排水溢流：flow / pressure 随进度持续攀升
        effects.put(AnomalyScenario.DRAINAGE_OVERFLOW, drainageOverflowScenario);
        // 第三方破坏：vibration 突变并叠加随机冲击
        effects.put(AnomalyScenario.THIRD_PARTY_DAMAGE, thirdPartyDamageScenario);
        // 温度飙升：温度随进度持续异常升高
        effects.put(AnomalyScenario.TEMP_SPIKE, (data, progress) -> {
            double base = data.getTemperature() == null ? 20.0 : data.getTemperature();
            data.setTemperature(round2(base + progress * 35.0));
            data.setStatus(progress > 0.7 ? 2 : 1);
        });
    }

    /**
     * 触发异常场景（默认持续时长）。
     *
     * @param deviceId 目标设备ID
     * @param scenario 场景类型
     */
    public void trigger(String deviceId, AnomalyScenario scenario) {
        trigger(deviceId, scenario, scenario.getDefaultDurationSeconds());
    }

    /**
     * 触发异常场景（指定持续时长）。
     *
     * @param deviceId        目标设备ID
     * @param scenario        场景类型
     * @param durationSeconds 持续时长（秒）
     */
    public void trigger(String deviceId, AnomalyScenario scenario, int durationSeconds) {
        long now = System.currentTimeMillis();
        activeScenarios.put(deviceId,
                new ActiveScenario(scenario, now, now + durationSeconds * 1000L));
        log.info("注入异常场景: deviceId={}, scenario={}, 持续{}秒",
                deviceId, scenario.getCode(), durationSeconds);
    }

    /**
     * 清除指定设备的异常场景。
     *
     * @param deviceId 设备ID
     * @return true 表示存在并已清除
     */
    public boolean clear(String deviceId) {
        ActiveScenario removed = activeScenarios.remove(deviceId);
        if (removed != null) {
            log.info("清除异常场景: deviceId={}, scenario={}", deviceId, removed.scenario().getCode());
        }
        return removed != null;
    }

    /**
     * 清除全部异常场景。
     *
     * @return 被清除的场景数量
     */
    public int clearAll() {
        int size = activeScenarios.size();
        activeScenarios.clear();
        log.info("清除全部异常场景, 共{}个", size);
        return size;
    }

    /**
     * 若设备存在激活中的异常场景，将效果叠加到传感数据；到期自动清除。
     *
     * @param data 传感数据（原地修改）
     */
    public void applyIfActive(SensorData data) {
        ActiveScenario active = activeScenarios.get(data.getDeviceId());
        if (active == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (active.expired(now)) {
            activeScenarios.remove(data.getDeviceId());
            log.info("异常场景到期自动清除: deviceId={}, scenario={}",
                    data.getDeviceId(), active.scenario().getCode());
            return;
        }
        ScenarioEffect effect = effects.get(active.scenario());
        if (effect != null) {
            effect.apply(data, active.progress(now));
        }
    }

    /**
     * 查询当前激活的全部异常场景（只读视图）。
     *
     * @return 设备ID -> 激活场景
     */
    public Map<String, ActiveScenario> listActive() {
        return Map.copyOf(activeScenarios);
    }

    /**
     * 判断指定设备是否处于异常场景中。
     *
     * @param deviceId 设备ID
     * @return true 表示存在激活场景
     */
    public boolean isActive(String deviceId) {
        return activeScenarios.containsKey(deviceId);
    }

    /**
     * 获取激活场景数量。
     *
     * @return 场景数量
     */
    public int activeCount() {
        return activeScenarios.size();
    }

    /**
     * 批量注册自定义场景效果（供扩展使用）。
     *
     * @param scenario 场景类型
     * @param effect   效果策略
     */
    public void registerEffect(AnomalyScenario scenario, ScenarioEffect effect) {
        effects.put(scenario, effect);
    }

    /**
     * 保留两位小数。
     *
     * @param value 原始值
     * @return 四舍五入到两位小数的值
     */
    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /**
     * 获取全部支持的场景类型列表。
     *
     * @return 场景类型数组
     */
    public static List<AnomalyScenario> supportedScenarios() {
        return List.of(AnomalyScenario.values());
    }
}
