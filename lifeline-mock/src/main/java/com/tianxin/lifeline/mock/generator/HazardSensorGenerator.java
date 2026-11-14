package com.tianxin.lifeline.mock.generator;

import com.tianxin.lifeline.common.core.constant.PipeType;
import com.tianxin.lifeline.mock.model.SensorData;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 危废管网数据生成器（设备短码 STR）。
 *
 * <p>正常指标区间：
 * <ul>
 *     <li>压力 pressure：0.2 ~ 0.5 MPa</li>
 *     <li>温度 temperature：15 ~ 40 ℃</li>
 * </ul>
 *
 * <p>危废介质具腐蚀性，腐蚀度指标整体高于其他管网。
 */
@Component
public class HazardSensorGenerator extends BaseGenerator {

    /** 压力下限（MPa） */
    private static final double PRESSURE_MIN = 0.2;
    /** 压力上限（MPa） */
    private static final double PRESSURE_MAX = 0.5;
    /** 温度下限（℃） */
    private static final double TEMPERATURE_MIN = 15.0;
    /** 温度上限（℃） */
    private static final double TEMPERATURE_MAX = 40.0;

    @Override
    public PipeType supportedPipeType() {
        return PipeType.HAZARD;
    }

    @Override
    protected String defaultSensorType() {
        return "corrosion";
    }

    @Override
    protected void fillNormalMetrics(SensorData data, double diurnal) {
        data.setPressure(round2(inRange(PRESSURE_MIN, PRESSURE_MAX, diurnal)));
        data.setTemperature(round2(inRange(TEMPERATURE_MIN, TEMPERATURE_MAX, diurnal)));
        data.setFlow(round2(5.0 + ThreadLocalRandom.current().nextDouble() * 25.0));
        data.setVibration(round2(0.2 + ThreadLocalRandom.current().nextDouble() * 0.6));
        // 危废腐蚀性强，腐蚀度偏高
        data.setCorrosion(round2(0.05 + ThreadLocalRandom.current().nextDouble() * 0.15));
        data.setLeakage(round2(ThreadLocalRandom.current().nextDouble() * 0.03));
    }
}
