package com.tianxin.lifeline.mock.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Mock 系统 YAML 配置属性绑定（前缀 {@code mock}）。
 *
 * <p>配置示例：
 * <pre>
 * mock:
 *   enabled: true
 *   sensor:
 *     count: 10
 *     report-interval-seconds: 30
 *     pipe-types: GAS,WAT,DRN,HTG,STR
 *   anomaly:
 *     auto-trigger: false
 *     types: PRESSURE_DROP,GAS_LEAK,TEMP_SPIKE,HEATING_LEAK,DRAINAGE_OVERFLOW,THIRD_PARTY_DAMAGE
 *   stress:
 *     mode: false
 *     device-count: 10000
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "mock")
public class MockProperties {

    /** 是否启用 Mock 数据生成 */
    private boolean enabled = true;

    /** 传感器相关配置 */
    private Sensor sensor = new Sensor();

    /** 异常场景相关配置 */
    private Anomaly anomaly = new Anomaly();

    /** 压测相关配置 */
    private Stress stress = new Stress();

    /**
     * 传感器配置项。
     */
    @Data
    public static class Sensor {

        /** 模拟设备总数 */
        private int count = 10;

        /** 上报周期（秒） */
        private int reportIntervalSeconds = 30;

        /** 管网类型编码列表：GAS/WAT/DRN/HTG/STR */
        private List<String> pipeTypes = List.of("GAS", "WAT", "DRN", "HTG", "STR");
    }

    /**
     * 异常场景配置项。
     */
    @Data
    public static class Anomaly {

        /** 是否自动随机触发异常场景 */
        private boolean autoTrigger = false;

        /** 允许自动触发的异常场景类型列表 */
        private List<String> types = List.of("PRESSURE_DROP", "GAS_LEAK", "TEMP_SPIKE",
                "HEATING_LEAK", "DRAINAGE_OVERFLOW", "THIRD_PARTY_DAMAGE");
    }

    /**
     * 压测模式配置项。
     *
     * <p>开启后设备数切换为 {@link #deviceCount}（1万/5万/10万分档），
     * 每批 100 条数据合并为单条 Kafka JSON Array 消息上报。
     */
    @Data
    public static class Stress {

        /** 是否开启压测模式 */
        private boolean mode = false;

        /** 压测设备数：10000 / 50000 / 100000 */
        private int deviceCount = 10000;
    }
}
