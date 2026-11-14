package com.tianxin.lifeline.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 网关路由配置。
 *
 * <p>按开发任务书定义的10个微服务配置路由规则，
 * 路径匹配后通过 Nacos 负载均衡（lb://）转发到对应服务。
 */
@Configuration
public class RouteConfig {

    /** 免鉴权白名单路径（登录、文档、监控端点等） */
    public static final List<String> AUTH_WHITELIST = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/**",
            "/ws/**"
    );

    /**
     * 定义全平台路由规则。
     *
     * @param builder 路由构建器
     * @return 路由定位器
     */
    @Bean
    public RouteLocator lifelineRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // 系统用户权限服务
                .route("sys-auth-service", r -> r.path("/api/v1/auth/**")
                        .uri("lb://sys-auth-service"))
                // 管网资产服务
                .route("pipe-asset-service", r -> r.path("/api/v1/pipes/**", "/api/v1/manholes/**", "/api/v1/devices/**")
                        .uri("lb://pipe-asset-service"))
                // 监测数据服务（含 WebSocket 推送通道）
                .route("monitor-data-service", r -> r.path("/api/v1/monitor/**", "/ws/monitor/**")
                        .uri("lb:ws://monitor-data-service"))
                // 预警预报服务
                .route("warning-service", r -> r.path("/api/v1/warnings/**", "/api/v1/forecasts/**", "/api/v1/warning-rules/**")
                        .uri("lb://warning-service"))
                // 预案处置服务
                .route("plan-service", r -> r.path("/api/v1/plans/**")
                        .uri("lb://plan-service"))
                // 施工监管服务
                .route("construction-service", r -> r.path("/api/v1/constructions/**")
                        .uri("lb://construction-service"))
                // 运维工单服务
                .route("operation-service", r -> r.path("/api/v1/orders/**", "/api/v1/inspections/**")
                        .uri("lb://operation-service"))
                // 区块链溯源服务
                .route("blockchain-service", r -> r.path("/api/v1/blockchain/**")
                        .uri("lb://blockchain-service"))
                // AI能力网关服务
                .route("ai-gateway-service", r -> r.path("/api/v1/ai/**")
                        .uri("lb://ai-gateway-service"))
                // 数字孪生接口服务（兼容 /api/v1/digital-twin/** 别名，重写为 /api/v1/twin/**）
                .route("digital-twin-service", r -> r.path("/api/v1/twin/**", "/api/v1/digital-twin/**")
                        .filters(f -> f.rewritePath("/api/v1/digital-twin/(?<remaining>.*)", "/api/v1/twin/${remaining}"))
                        .uri("lb://digital-twin-service"))
                .build();
    }
}
