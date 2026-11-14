package com.tianxin.lifeline.mock.scenario;

import com.tianxin.lifeline.mock.model.SensorData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 第三方破坏异常场景实现（{@link AnomalyScenario#THIRD_PARTY_DAMAGE}）。
 *
 * <p>模拟外部施工机械撞击/钻探管体的突发特征：
 * <ul>
 *     <li>vibration 幅值瞬间突变至高位（最大倍数可参数化，默认 12 倍），
 *         并叠加随机抖动模拟持续施工冲击；</li>
 *     <li>冲击伴随局部压力波动，pressure 小幅抖动（±10%）；</li>
 *     <li>振动倍数超过报警阈值（默认 6 倍）时设备状态置为报警（2），否则异常（1）。</li>
 * </ul>
 *
 * <p>持续时长由 {@code ScenarioManager#trigger} 的 durationSeconds 参数化，
 * 强度由 {@link #maxVibrationMultiplier} 参数化。
 */
@Component
public class ThirdPartyDamageScenario implements ScenarioManager.ScenarioEffect {

    /** 突变阶段占比（前 10% 时长完成振动突变，其后持续冲击） */
    private static final double SPIKE_PHASE_RATIO = 0.1;

    /** 默认振动基准（mm/s，数据未携带振动时的兜底值） */
    private static final double DEFAULT_VIBRATION = 0.5;

    /** 压力抖动幅度（±10%） */
    private static final double PRESSURE_JITTER_RATIO = 0.1;

    /** 最大振动倍数（相对基准幅值） */
    private final double maxVibrationMultiplier;

    /** 振动报警倍数阈值（超过置报警状态） */
    private final double alarmMultiplier;

    /**
     * 默认强度构造（Spring 注入使用）。
     */
    @Autowired
    public ThirdPartyDamageScenario() {
        this(12.0, 6.0);
    }

    /**
     * 自定义强度构造。
     *
     * @param maxVibrationMultiplier 最大振动倍数
     * @param alarmMultiplier        振动报警倍数阈值
     */
    public ThirdPartyDamageScenario(double maxVibrationMultiplier, double alarmMultiplier) {
        this.maxVibrationMultiplier = maxVibrationMultiplier;
        this.alarmMultiplier = alarmMultiplier;
    }

    /**
     * 将第三方破坏效果叠加到传感数据。
     *
     * @param data     原始传感数据（原地修改 vibration / pressure / status）
     * @param progress 场景演进进度，0~1
     */
    @Override
    public void apply(SensorData data, double progress) {
        double severity = Math.min(progress / SPIKE_PHASE_RATIO, 1.0);
        double base = data.getVibration() == null ? DEFAULT_VIBRATION : data.getVibration();
        // 随机抖动 ±15%，模拟施工冲击的不规律性
        double jitter = 0.85 + ThreadLocalRandom.current().nextDouble() * 0.30;
        double multiplier = 1.0 + (maxVibrationMultiplier - 1.0) * severity;
        data.setVibration(round2(Math.max(base, base * multiplier * jitter)));

        if (data.getPressure() != null) {
            double pressureJitter = 1.0 + (ThreadLocalRandom.current().nextDouble() * 2 - 1)
                    * PRESSURE_JITTER_RATIO * severity;
            data.setPressure(round2(data.getPressure() * pressureJitter));
        }

        data.setStatus(multiplier >= alarmMultiplier ? 2 : 1);
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
}
