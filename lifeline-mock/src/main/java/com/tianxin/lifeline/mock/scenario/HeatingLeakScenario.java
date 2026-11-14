package com.tianxin.lifeline.mock.scenario;

import com.tianxin.lifeline.mock.model.SensorData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 供热泄漏异常场景实现（{@link AnomalyScenario#HEATING_LEAK}）。
 *
 * <p>模拟供热管网失水泄漏的复合特征：
 * <ul>
 *     <li>前 25% 时长内 flow 骤降（最大降幅可参数化，默认 70%）并维持低位；</li>
 *     <li>供水温度随介质流失异常跌落（最大降幅可参数化，默认 25℃，不低于 5℃）；</li>
 *     <li>流量降幅超过 50% 时设备状态置为报警（2），否则异常（1）。</li>
 * </ul>
 *
 * <p>持续时长由 {@code ScenarioManager#trigger} 的 durationSeconds 参数化，
 * 强度由 {@link #maxFlowDropRatio} / {@link #maxTemperatureDrop} 参数化。
 */
@Component
public class HeatingLeakScenario implements ScenarioManager.ScenarioEffect {

    /** 骤降阶段占比（前 25% 时长完成流量骤降） */
    private static final double DROP_PHASE_RATIO = 0.25;

    /** 温度跌落下限（℃，防止出现不合理负温） */
    private static final double TEMPERATURE_FLOOR = 5.0;

    /** 默认供水温度基准（℃，数据未携带温度时的兜底值） */
    private static final double DEFAULT_TEMPERATURE = 65.0;

    /** 默认流量基准（m³/h，数据未携带流量时的兜底值） */
    private static final double DEFAULT_FLOW = 90.0;

    /** 最大流量降幅（0~1） */
    private final double maxFlowDropRatio;

    /** 最大温度降幅（℃） */
    private final double maxTemperatureDrop;

    /**
     * 默认强度构造（Spring 注入使用）。
     */
    @Autowired
    public HeatingLeakScenario() {
        this(0.7, 25.0);
    }

    /**
     * 自定义强度构造。
     *
     * @param maxFlowDropRatio   最大流量降幅（0~1）
     * @param maxTemperatureDrop 最大温度降幅（℃）
     */
    public HeatingLeakScenario(double maxFlowDropRatio, double maxTemperatureDrop) {
        this.maxFlowDropRatio = maxFlowDropRatio;
        this.maxTemperatureDrop = maxTemperatureDrop;
    }

    /**
     * 将供热泄漏效果叠加到传感数据。
     *
     * @param data     原始传感数据（原地修改 flow / temperature / status）
     * @param progress 场景演进进度，0~1
     */
    @Override
    public void apply(SensorData data, double progress) {
        double severity = Math.min(progress / DROP_PHASE_RATIO, 1.0);
        double dropRatio = maxFlowDropRatio * severity;

        double flow = data.getFlow() == null ? DEFAULT_FLOW : data.getFlow();
        data.setFlow(round2(flow * (1.0 - dropRatio)));

        double temperature = data.getTemperature() == null ? DEFAULT_TEMPERATURE : data.getTemperature();
        double cooled = Math.max(TEMPERATURE_FLOOR, temperature - maxTemperatureDrop * severity);
        data.setTemperature(round2(cooled));

        data.setStatus(dropRatio > 0.5 ? 2 : 1);
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
