package com.tianxin.lifeline.mock.scenario;

import com.tianxin.lifeline.mock.model.SensorData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 燃气泄漏异常场景实现（{@link AnomalyScenario#GAS_LEAK}）。
 *
 * <p>模拟燃气管网泄漏的复合特征：
 * <ul>
 *     <li>leakage 浓度从微量陡升至危险值（最大升幅可参数化，默认 25 %LEL）；</li>
 *     <li>伴随管存量流失，pressure 同步下降（最大降幅可参数化，默认 40%）；</li>
 *     <li>leakage 超过报警阈值（默认 15）时设备状态置为报警（2），否则异常（1）。</li>
 * </ul>
 *
 * <p>持续时长由 {@code ScenarioManager#trigger} 的 durationSeconds 参数化，
 * 强度由 {@link #maxLeakageRise} / {@link #maxPressureDropRatio} 参数化。
 */
@Component
public class GasLeakScenario implements ScenarioManager.ScenarioEffect {

    /** 陡升阶段占比（前 30% 时长完成浓度陡升） */
    private static final double RISE_PHASE_RATIO = 0.3;

    /** leakage 基准值（微量背景浓度） */
    private static final double LEAKAGE_BASE = 0.3;

    /** leakage 报警阈值（超过置报警状态） */
    private final double leakageAlarmThreshold;

    /** leakage 最大升幅（%LEL 近似值） */
    private final double maxLeakageRise;

    /** 最大压力降幅（40% 表示压力跌至原值的 60%） */
    private final double maxPressureDropRatio;

    /**
     * 默认强度构造（Spring 注入使用）。
     */
    @Autowired
    public GasLeakScenario() {
        this(25.0, 0.4, 15.0);
    }

    /**
     * 自定义强度构造。
     *
     * @param maxLeakageRise        leakage 最大升幅
     * @param maxPressureDropRatio  最大压力降幅（0~1）
     * @param leakageAlarmThreshold leakage 报警阈值
     */
    public GasLeakScenario(double maxLeakageRise, double maxPressureDropRatio, double leakageAlarmThreshold) {
        this.maxLeakageRise = maxLeakageRise;
        this.maxPressureDropRatio = maxPressureDropRatio;
        this.leakageAlarmThreshold = leakageAlarmThreshold;
    }

    /**
     * 将燃气泄漏效果叠加到传感数据。
     *
     * @param data     原始传感数据（原地修改 leakage / pressure / status）
     * @param progress 场景演进进度，0~1
     */
    @Override
    public void apply(SensorData data, double progress) {
        double severity = Math.min(progress / RISE_PHASE_RATIO, 1.0);
        double leakage = LEAKAGE_BASE + maxLeakageRise * severity;
        data.setLeakage(round2(leakage));

        double pressure = data.getPressure() == null ? 0.3 : data.getPressure();
        double dropped = pressure * (1.0 - maxPressureDropRatio * severity);
        data.setPressure(round2(dropped));

        data.setStatus(leakage >= leakageAlarmThreshold ? 2 : 1);
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
