package com.tianxin.lifeline.mock.scenario;

import com.tianxin.lifeline.mock.model.SensorData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 排水溢流异常场景实现（{@link AnomalyScenario#DRAINAGE_OVERFLOW}）。
 *
 * <p>模拟暴雨/堵塞导致排水管网液位与流量持续攀升的渐进特征：
 * <ul>
 *     <li>flow 随进度线性攀升（最大升幅可参数化，默认 +150%）；</li>
 *     <li>pressure（液位近似指标）同步持续抬升（最大升幅可参数化，默认 +0.6 MPa）；</li>
 *     <li>进度超过 60% 进入溢流高危阶段，设备状态置为报警（2），否则异常（1）。</li>
 * </ul>
 *
 * <p>持续时长由 {@code ScenarioManager#trigger} 的 durationSeconds 参数化，
 * 强度由 {@link #maxFlowRiseRatio} / {@link #maxPressureRise} 参数化。
 */
@Component
public class DrainageOverflowScenario implements ScenarioManager.ScenarioEffect {

    /** 溢流高危阶段起点（进度超过该值置报警状态） */
    private static final double OVERFLOW_ALARM_PROGRESS = 0.6;

    /** 默认流量基准（m³/h，数据未携带流量时的兜底值） */
    private static final double DEFAULT_FLOW = 60.0;

    /** 默认压力基准（MPa，数据未携带压力时的兜底值） */
    private static final double DEFAULT_PRESSURE = 0.1;

    /** 最大流量升幅比例（1.5 表示流量升至原值的 250%） */
    private final double maxFlowRiseRatio;

    /** 最大压力抬升量（MPa） */
    private final double maxPressureRise;

    /**
     * 默认强度构造（Spring 注入使用）。
     */
    @Autowired
    public DrainageOverflowScenario() {
        this(1.5, 0.6);
    }

    /**
     * 自定义强度构造。
     *
     * @param maxFlowRiseRatio 最大流量升幅比例（>0）
     * @param maxPressureRise  最大压力抬升量（MPa）
     */
    public DrainageOverflowScenario(double maxFlowRiseRatio, double maxPressureRise) {
        this.maxFlowRiseRatio = maxFlowRiseRatio;
        this.maxPressureRise = maxPressureRise;
    }

    /**
     * 将排水溢流效果叠加到传感数据。
     *
     * @param data     原始传感数据（原地修改 flow / pressure / status）
     * @param progress 场景演进进度，0~1
     */
    @Override
    public void apply(SensorData data, double progress) {
        double flow = data.getFlow() == null ? DEFAULT_FLOW : data.getFlow();
        data.setFlow(round2(flow * (1.0 + maxFlowRiseRatio * progress)));

        double pressure = data.getPressure() == null ? DEFAULT_PRESSURE : data.getPressure();
        data.setPressure(round2(pressure + maxPressureRise * progress));

        data.setStatus(progress > OVERFLOW_ALARM_PROGRESS ? 2 : 1);
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
