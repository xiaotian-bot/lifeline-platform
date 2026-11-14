package com.tianxin.lifeline.asset;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 管网资产服务启动类。
 *
 * <p>职责：管网/设备/井室台账管理、PostGIS 空间查询、GeoJSON 输出、
 * 一管一档全生命周期档案。端口 9011。
 */
@EnableDiscoveryClient
@MapperScan("com.tianxin.lifeline.asset.mapper")
@SpringBootApplication
public class PipeAssetApplication {

    /**
     * 服务入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(PipeAssetApplication.class, args);
    }
}
