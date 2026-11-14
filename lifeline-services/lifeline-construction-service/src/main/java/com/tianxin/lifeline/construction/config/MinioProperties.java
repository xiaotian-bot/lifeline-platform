package com.tianxin.lifeline.construction.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MinIO 对象存储配置（环境变量占位，部署时注入）。
 *
 * <p>施工影像桶与 deploy/docker-compose.yml 的 minio 服务对齐。
 */
@Data
@Component
@ConfigurationProperties(prefix = "lifeline.minio")
public class MinioProperties {

    /** MinIO 服务端点 */
    private String endpoint = "http://localhost:9000";

    /** 访问密钥 */
    private String accessKey = "minioadmin";

    /** 私钥 */
    private String secretKey = "minioadmin123";

    /** 施工影像桶名 */
    private String bucket = "lifeline-construction";
}
