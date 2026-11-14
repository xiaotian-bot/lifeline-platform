package com.tianxin.lifeline.common.web.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3（springdoc）接口文档统一配置。
 *
 * <p>各服务引入本模块后自动生效，文档地址：/v3/api-docs，UI 地址：/swagger-ui.html。
 */
@Configuration
public class OpenApiConfig {

    /** Bearer Token 安全方案名称 */
    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    /**
     * 构建平台级 OpenAPI 文档描述。
     *
     * @return OpenAPI 文档对象
     */
    @Bean
    public OpenAPI lifelineOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("安塞区城市安全生命线管网AI智慧平台 API")
                        .description("城市安全生命线（供水/供暖/燃气/污水/危废）管网数字化、实时监测、智能预警与处置闭环平台接口文档")
                        .version("v1.0.0")
                        .contact(new Contact().name("天信研发团队").email("dev@tianxin.com")))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("输入登录获取的 JWT Access Token")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}
