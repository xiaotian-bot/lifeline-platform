package com.tianxin.lifeline.mock.generator;

import com.tianxin.lifeline.common.core.constant.PipeType;
import com.tianxin.lifeline.mock.model.SensorData;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 供暖管网数据生成器（设备短码 HTG）。
 *
 * <p>正常指标区间：
 * <ul>
 *     <li>温度 temperature：40 ~ 90 ℃</li>
 *     <li>压力 pressure：0.3 ~ 0.6 MPa</li>
 *     <li>流量 flow：30 ~ 150 m³/h</li>
 * </ul>
 */
@Component
public class HeatSensorGenerator extends BaseGenerator {

    /** 温度下限（℃） */
    private static final double TEMPERATURE_MIN = 40.0;
    /** 温度上限（℃） */
    private static final double TEMPERATURE_MAX = 90.0;
    /** 压力下限（MPa） */
    private static final double PRESSURE_MIN = 0.3;
    /** 压力上限（MPa） */
    private static final double PRESSURE_MAX = 0.6;
    /** 流量下限（m³/h） */
    private static final double FLOW_MIN = 30.0;
    /** 流量上限（m³/h） */
    private static final double FLOW_MAX = 150.0;

    @Override
    public PipeType supportedPipeType() {
        return PipeType.HEAT;
    }

    @Override
    protected String defaultSensorType() {
        return "temperature";
    }

    @Override
    protected void fillNormalMetrics(SensorData data, double diurnal) {
        data.setTemperature(round2(inRange(TEMPERATURE_MIN, TEMPERATURE_MAX, diurnal)));
        data.setPressure(round2(inRange(PRESSURE_MIN, PRESSURE_MAX, diurnal)));
        data.setFlow(round2(inRange(FLOW_MIN, FLOW_MAX, diurnal)));
        data.setVibration(round2(0.3 + ThreadLocalRandom.current().nextDouble() * 1.0));
        data.setCorrosion(round2(0.02 + ThreadLocalRandom.current().nextDouble() * 0.06));
        data.setLeakage(0.0);
    }
}
