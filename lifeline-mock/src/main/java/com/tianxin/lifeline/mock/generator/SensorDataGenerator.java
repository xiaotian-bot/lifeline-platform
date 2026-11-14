package com.tianxin.lifeline.mock.generator;

import com.tianxin.lifeline.common.core.constant.PipeType;
import com.tianxin.lifeline.mock.model.DeviceInfo;
import com.tianxin.lifeline.mock.model.SensorData;

/**
 * 传感器数据生成器接口（策略模式）。
 *
 * <p>每种管网类型对应一个独立实现，按各自的物理量纲与正常区间
 * 生成仿真传感数据：
 * <ul>
 *     <li>{@link WaterSensorGenerator} 供水管网</li>
 *     <li>{@link GasSensorGenerator} 燃气管网</li>
 *     <li>{@link HeatSensorGenerator} 供暖管网</li>
 *     <li>{@link SewageSensorGenerator} 污水管网</li>
 *     <li>{@link HazardSensorGenerator} 危废管网</li>
 * </ul>
 */
public interface SensorDataGenerator {

    /**
     * 当前生成器支持的管网类型。
     *
     * @return 管网类型
     */
    PipeType supportedPipeType();

    /**
     * 生成一条传感数据。
     *
     * @param device 设备信息
     * @param ts     采集时间戳（毫秒）
     * @return 传感数据
     */
    SensorData generate(DeviceInfo device, long ts);
}
