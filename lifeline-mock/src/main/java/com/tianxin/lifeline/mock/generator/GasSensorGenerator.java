package com.tianxin.lifeline.mock.generator;

import com.tianxin.lifeline.common.core.constant.PipeType;
import com.tianxin.lifeline.mock.model.SensorData;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 燃气管网数据生成器（设备短码 GAS）。
 *
 * <p>正常指标区间：
 * <ul>
 *     <li>压力 pressure：0.1 ~ 0.4 MPa</li>
 *     <li>温度 temperature：5 ~ 30 ℃</li>
 *     <li>流量 flow：10 ~ 100 m³/h</li>
 *     <li>泄漏 leakage：正常状态接近 0（0 ~ 0.05）</li>
 * </ul>
 */
@Component
public class GasSensorGenerator extends BaseGenerator {

    /** 压力下限（MPa） */
    private static final double PRESSURE_MIN = 0.1;
    /** 压力上限（MPa） */
    private static final double PRESSURE_MAX = 0.4;
    /** 温度下限（℃） */
    private static final double TEMPERATURE_MIN = 5.0;
    /** 温度上限（℃） */
    private static final double TEMPERATURE_MAX = 30.0;
    /** 流量下限（m³/h） */
    private static final double FLOW_MIN = 10.0;
    /** 流量上限（m³/h） */
    private static final double FLOW_MAX = 100.0;

    @Override
    public PipeType supportedPipeType() {
        return PipeType.GAS;
    }

    @Override
    protected String defaultSensorType() {
        return "leakage";
    }

    @Override
    protected void fillNormalMetrics(SensorData data, double diurnal) {
        data.setPressure(round2(inRange(PRESSURE_MIN, PRESSURE_MAX, diurnal)));
        data.setTemperature(round2(inRange(TEMPERATURE_MIN, TEMPERATURE_MAX, diurnal)));
        data.setFlow(round2(inRange(FLOW_MIN, FLOW_MAX, diurnal)));
        // 正常状态泄漏浓度接近 0
        data.setLeakage(round2(ThreadLocalRandom.current().nextDouble() * 0.05));
        data.setVibration(round2(0.1 + ThreadLocalRandom.current().nextDouble() * 0.5));
        data.setCorrosion(round2(0.01 + ThreadLocalRandom.current().nextDouble() * 0.03));
    }
}
