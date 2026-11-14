package com.tianxin.lifeline.mock.generator;

import com.tianxin.lifeline.common.core.constant.PipeType;
import com.tianxin.lifeline.mock.model.SensorData;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 供水管网数据生成器（设备短码 WAT）。
 *
 * <p>正常指标区间：
 * <ul>
 *     <li>压力 pressure：0.2 ~ 0.8 MPa</li>
 *     <li>温度 temperature：10 ~ 25 ℃</li>
 *     <li>流量 flow：50 ~ 200 m³/h</li>
 * </ul>
 */
@Component
public class WaterSensorGenerator extends BaseGenerator {

    /** 压力下限（MPa） */
    private static final double PRESSURE_MIN = 0.2;
    /** 压力上限（MPa） */
    private static final double PRESSURE_MAX = 0.8;
    /** 温度下限（℃） */
    private static final double TEMPERATURE_MIN = 10.0;
    /** 温度上限（℃） */
    private static final double TEMPERATURE_MAX = 25.0;
    /** 流量下限（m³/h） */
    private static final double FLOW_MIN = 50.0;
    /** 流量上限（m³/h） */
    private static final double FLOW_MAX = 200.0;

    @Override
    public PipeType supportedPipeType() {
        return PipeType.WATER;
    }

    @Override
    protected String defaultSensorType() {
        return "pressure";
    }

    @Override
    protected void fillNormalMetrics(SensorData data, double diurnal) {
        data.setPressure(round2(inRange(PRESSURE_MIN, PRESSURE_MAX, diurnal)));
        data.setTemperature(round2(inRange(TEMPERATURE_MIN, TEMPERATURE_MAX, diurnal)));
        data.setFlow(round2(inRange(FLOW_MIN, FLOW_MAX, diurnal)));
        data.setVibration(round2(0.2 + ThreadLocalRandom.current().nextDouble() * 0.8));
        data.setCorrosion(round2(0.01 + ThreadLocalRandom.current().nextDouble() * 0.04));
        data.setLeakage(0.0);
    }
}
