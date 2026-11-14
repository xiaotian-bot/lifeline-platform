package com.tianxin.lifeline.mock.generator;

import com.tianxin.lifeline.common.core.constant.PipeType;
import com.tianxin.lifeline.mock.model.SensorData;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 污水管网数据生成器（设备短码 DRN）。
 *
 * <p>正常指标区间：
 * <ul>
 *     <li>流量 flow：20 ~ 100 m³/h</li>
 *     <li>温度 temperature：10 ~ 25 ℃</li>
 * </ul>
 *
 * <p>污水以重力自流为主，压力维持低位；早晚用水高峰带动流量波动。
 */
@Component
public class SewageSensorGenerator extends BaseGenerator {

    /** 流量下限（m³/h） */
    private static final double FLOW_MIN = 20.0;
    /** 流量上限（m³/h） */
    private static final double FLOW_MAX = 100.0;
    /** 温度下限（℃） */
    private static final double TEMPERATURE_MIN = 10.0;
    /** 温度上限（℃） */
    private static final double TEMPERATURE_MAX = 25.0;

    @Override
    public PipeType supportedPipeType() {
        return PipeType.SEWAGE;
    }

    @Override
    protected String defaultSensorType() {
        return "flow";
    }

    @Override
    protected void fillNormalMetrics(SensorData data, double diurnal) {
        data.setFlow(round2(inRange(FLOW_MIN, FLOW_MAX, diurnal)));
        data.setTemperature(round2(inRange(TEMPERATURE_MIN, TEMPERATURE_MAX, diurnal)));
        // 污水重力自流，压力低位
        data.setPressure(round2(0.05 + ThreadLocalRandom.current().nextDouble() * 0.15));
        data.setVibration(round2(0.1 + ThreadLocalRandom.current().nextDouble() * 0.4));
        data.setCorrosion(round2(0.03 + ThreadLocalRandom.current().nextDouble() * 0.07));
        data.setLeakage(0.0);
    }
}
