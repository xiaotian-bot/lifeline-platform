package com.tianxin.lifeline.mock.scenario;

import com.tianxin.lifeline.mock.model.SensorData;
import org.springframework.stereotype.Component;

/**
 * 压降异常场景实现（{@link AnomalyScenario#PRESSURE_DROP}）。
 *
 * <p>模拟管道破裂泄漏导致的压力骤降：
 * <ul>
 *     <li>前 20% 时长内压力快速跌落（最大降幅 85%）；</li>
 *     <li>随后维持低压状态直至场景结束；</li>
 *     <li>压力降幅超过 50% 时设备状态置为报警（2），否则异常（1）。</li>
 * </ul>
 */
@Component
public class PressureDropScenario implements ScenarioManager.ScenarioEffect {

    /** 快速跌落阶段占比（前 20% 时长完成跌落） */
    private static final double DROP_PHASE_RATIO = 0.2;

    /** 最大压力降幅（85%） */
    private static final double MAX_DROP_RATIO = 0.85;

    /**
     * 将压降效果叠加到传感数据。
     *
     * @param data     原始传感数据（原地修改 pressure 与 status）
     * @param progress 场景演进进度，0~1
     */
    @Override
    public void apply(SensorData data, double progress) {
        double severity = Math.min(progress / DROP_PHASE_RATIO, 1.0);
        double dropRatio = MAX_DROP_RATIO * severity;
        double pressure = data.getPressure() == null ? 0.4 : data.getPressure();
        double dropped = pressure * (1.0 - dropRatio);
        data.setPressure(Math.round(dropped * 1000.0) / 1000.0);
        data.setStatus(dropRatio > 0.5 ? 2 : 1);
    }
}
