package com.tianxin.lifeline.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 6.x 配置。
 *
 * <p>本平台采用「网关统一鉴权 + 各服务 JWT 过滤器（lifeline-common-security 的
 * JwtAuthFilter）解析身份」的架构，服务自身不维护 Session，因此：
 * <ul>
 *     <li>禁用 CSRF（无状态 API）</li>
 *     <li>会话策略设为 STATELESS</li>
 *     <li>放行全部请求，登录态与权限校验由控制器结合 SecurityContext 显式判断</li>
 * </ul>
 * 同时注册 BCrypt 密码编码器用于登录比对与用户创建。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 安全过滤器链：无状态 + 全放行。
     *
     * @param http HttpSecurity 构建器
     * @return 过滤器链
     * @throws Exception 构建异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    /**
     * BCrypt 密码编码器（与种子数据中的 $2a$ 哈希兼容）。
     *
     * @return 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
