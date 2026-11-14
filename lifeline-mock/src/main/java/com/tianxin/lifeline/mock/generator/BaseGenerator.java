package com.tianxin.lifeline.mock.generator;

import com.tianxin.lifeline.mock.model.DeviceInfo;
import com.tianxin.lifeline.mock.model.SensorData;

import java.time.LocalTime;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 传感数据生成器基类。
 *
 * <p>封装各管网生成器的通用逻辑（模板方法模式）：
 * <ul>
 *     <li><b>昼夜波动</b>：基于正弦函数模拟一天内的用量变化，
 *         正午达到峰值、凌晨跌入低谷；</li>
 *     <li><b>高斯噪声</b>：每次上报为各指标叠加小幅随机扰动，
 *         模拟真实传感器的测量抖动；</li>
 *     <li><b>生成骨架</b>：装配公共字段后交由子类填充各自指标。</li>
 * </ul>
 */
public abstract class BaseGenerator implements SensorDataGenerator {

    /** 默认高斯噪声比例（±2%） */
    protected static final double DEFAULT_NOISE_RATIO = 0.02;

    /**
     * 生成一条传感数据（模板方法）。
     *
     * @param device 设备信息
     * @param ts     采集时间戳（毫秒）
     * @return 传感数据
     */
    @Override
    public SensorData generate(DeviceInfo device, long ts) {
        SensorData data = new SensorData();
        data.setDeviceId(device.getDeviceId());
        data.setPipeType(supportedPipeType().getCode());
        data.setSensorType(device.getSensorType() != null ? device.getSensorType() : defaultSensorType());
        data.setAreaId(device.getAreaId());
        data.setTs(ts);
        data.setStatus(0);
        double diurnal = diurnalFactor(ts);
        fillNormalMetrics(data, diurnal);
        applyNoise(data);
        return data;
    }

    /**
     * 默认主传感器类型（设备未指定 sensorType 时使用）。
     *
     * @return 传感器类型编码
     */
    protected abstract String defaultSensorType();

    /**
     * 子类实现：按管网特征填充正常指标值。
     *
     * @param data    传感数据（原地填充）
     * @param diurnal 昼夜波动系数，0.2（凌晨低谷）~1.0（正午峰值）
     */
    protected abstract void fillNormalMetrics(SensorData data, double diurnal);

    /**
     * 计算昼夜波动系数。
     *
     * <p>以正弦曲线模拟：6 点起升、12 点峰值、18 点回落、0 点低谷，
     * 系数范围 0.2~1.0。
     *
     * @param ts 时间戳（毫秒）
     * @return 昼夜波动系数
     */
    protected double diurnalFactor(long ts) {
        LocalTime time = LocalTime.ofInstant(java.time.Instant.ofEpochMilli(ts),
                java.time.ZoneId.systemDefault());
        double hour = time.toSecondOfDay() / 3600.0;
        double sine = Math.sin((hour - 6.0) / 24.0 * 2.0 * Math.PI);
        return 0.6 + 0.4 * sine;
    }

    /**
     * 在指定区间内取值，并随昼夜波动系数在区间中上部浮动。
     *
     * @param min     区间下限
     * @param max     区间上限
     * @param diurnal 昼夜波动系数
     * @return 区间内的波动值
     */
    protected double inRange(double min, double max, double diurnal) {
        double position = 0.35 + 0.45 * diurnal;
        return min + (max - min) * position;
    }

    /**
     * 为传感数据的各项指标叠加高斯噪声。
     *
     * @param data 传感数据（原地修改）
     */
    protected void applyNoise(SensorData data) {
        data.setPressure(noise(data.getPressure()));
        data.setTemperature(noise(data.getTemperature()));
        data.setFlow(noise(data.getFlow()));
        data.setVibration(noise(data.getVibration()));
        data.setCorrosion(noise(data.getCorrosion()));
        data.setLeakage(noise(data.getLeakage()));
    }

    /**
     * 对单个指标值叠加默认比例（±2%）的高斯噪声。
     *
     * @param value 原始值，null 时返回 null
     * @return 叠加噪声后的值（保留两位小数，非负）
     */
    protected Double noise(Double value) {
        return noise(value, DEFAULT_NOISE_RATIO);
    }

    /**
     * 对单个指标值叠加指定比例的高斯噪声。
     *
     * @param value 原始值，null 时返回 null
     * @param ratio 噪声比例（如 0.02 表示 ±2% 标准差）
     * @return 叠加噪声后的值（保留两位小数，非负）
     */
    protected Double noise(Double value, double ratio) {
        if (value == null) {
            return null;
        }
        double noisy = value + ThreadLocalRandom.current().nextGaussian() * Math.max(value, 0.01) * ratio;
        return round2(Math.max(noisy, 0.0));
    }

    /**
     * 保留两位小数。
     *
     * @param value 原始值
     * @return 四舍五入到两位小数
     */
    protected static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
