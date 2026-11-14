package com.tianxin.lifeline.construction.config;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 客户端配置。
 *
 * <p>施工影像走 MinIO 流式上传（putObject 直接消费请求输入流，
 * 不落本地磁盘），DB 只存对象键。
 */
@Configuration
public class MinioConfig {

    /**
     * 构建 MinIO 客户端。
     *
     * @param properties MinIO 配置属性
     * @return MinIO 客户端
     */
    @Bean
    public MinioClient minioClient(MinioProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }
}
