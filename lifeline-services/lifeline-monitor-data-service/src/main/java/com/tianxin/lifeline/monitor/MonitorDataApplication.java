package com.tianxin.lifeline.monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 监测数据服务启动类。
 *
 * <p>MVP 数据链路核心服务：消费 Kafka Topic {@code pipe_sensor_raw} 的原始传感数据，
 * 经清洗后双写 TDengine（时序存储）与 PostgreSQL（设备最新状态），
 * 并同步维护 Redis 实时缓存，对外提供实时/历史/统计 REST 查询接口。
 *
 * <p>扫描 {@code com.tianxin.lifeline} 全量包，以纳入平台公共模块中的组件。
 */
@SpringBootApplication(scanBasePackages = "com.tianxin.lifeline")
public class MonitorDataApplication {

    /**
     * 服务入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(MonitorDataApplication.class, args);
    }
}
