package com.tianxin.lifeline.monitor.config;

import com.taosdata.jdbc.rs.RestfulDriver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import javax.sql.DataSource;

/**
 * TDengine 独立数据源配置。
 *
 * <p>TDengine 时序库使用独立 {@link DataSource} 与独立 {@link JdbcTemplate}
 * （Bean 名 {@code tdengineJdbcTemplate}），不与 PostgreSQL 主数据源混用，
 * 也不经过 MyBatis-Plus。
 *
 * <p>连接采用 RESTful 模式（{@link RestfulDriver}），JDBC URL 形如
 * {@code jdbc:TAOS-REST://host:port/pipe_ts}；驱动 3.x 实际接受的协议前缀为
 * {@code jdbc:TAOS-RS://}，此处自动做等价归一化。
 *
 * <p>同时显式注册 PostgreSQL 主 JdbcTemplate 并标记 {@code @Primary}：
 * 一旦容器中存在任何 JdbcTemplate Bean，Spring Boot 的 JdbcTemplate
 * 自动装配即失效，因此两个 JdbcTemplate 必须在此一并声明。
 */
@Configuration
public class TDengineConfig {

    /** TDengine RESTful 驱动接受的 URL 前缀（3.x 版本） */
    private static final String TAOS_RS_PREFIX = "jdbc:TAOS-RS://";

    /** 配置文件中使用的 URL 前缀（等价别名） */
    private static final String TAOS_REST_PREFIX = "jdbc:TAOS-REST://";

    /** TDengine JDBC URL */
    @Value("${tdengine.url:jdbc:TAOS-REST://localhost:6030/pipe_ts}")
    private String url;

    /** TDengine 用户名 */
    @Value("${tdengine.username:root}")
    private String username;

    /** TDengine 密码 */
    @Value("${tdengine.password:taosdata}")
    private String password;

    /**
     * TDengine 专用 JdbcTemplate（时序读写统一入口）。
     *
     * <p>内部持有独立的 {@link SimpleDriverDataSource}，但不将其注册为容器 Bean，
     * 避免容器中出现多 DataSource 导致主数据源注入歧义。
     *
     * @return TDengine JdbcTemplate
     */
    @Bean(name = "tdengineJdbcTemplate")
    public JdbcTemplate tdengineJdbcTemplate() {
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriverClass(RestfulDriver.class);
        dataSource.setUrl(normalizeUrl(url));
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return new JdbcTemplate(dataSource);
    }

    /**
     * PostgreSQL 主 JdbcTemplate（设备状态更新等关系型操作）。
     *
     * <p>标记 {@code @Primary}，注入未限定名称的 JdbcTemplate 时默认使用本实例。
     *
     * @param dataSource Spring Boot 自动装配的 PostgreSQL 主数据源
     * @return PostgreSQL JdbcTemplate
     */
    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * 归一化 TDengine JDBC URL：将 {@code jdbc:TAOS-REST://} 等价替换为
     * 驱动 3.x 实际识别的 {@code jdbc:TAOS-RS://} 前缀。
     *
     * @param rawUrl 原始配置 URL
     * @return 驱动可识别的 URL
     */
    private String normalizeUrl(String rawUrl) {
        if (rawUrl != null && rawUrl.startsWith(TAOS_REST_PREFIX)) {
            return TAOS_RS_PREFIX + rawUrl.substring(TAOS_REST_PREFIX.length());
        }
        return rawUrl;
    }
}
