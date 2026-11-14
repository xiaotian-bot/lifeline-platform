package com.tianxin.lifeline.mock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Mock 数据模拟系统启动类。
 *
 * <p>模拟安塞区城市安全生命线五大类管网（燃气/供水/污水/供暖/危废）
 * 的传感器数据生成，按固定周期将数据上报至 Kafka Topic
 * {@code pipe_sensor_raw}，供下游 Flink 清洗与预警链路消费。
 *
 * <p>服务端口：9100；提供 REST 控制台用于启停、异常场景注入与动态调参。
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class MockDataApplication {

    /**
     * 应用入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(MockDataApplication.class, args);
    }
}
