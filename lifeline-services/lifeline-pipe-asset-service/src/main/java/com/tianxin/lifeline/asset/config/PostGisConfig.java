package com.tianxin.lifeline.asset.config;

import org.springframework.context.annotation.Configuration;

/**
 * PostGIS 空间能力配置。
 *
 * <p>本服务不引入 postgis-jdbc 对象映射，而是通过 PostgreSQL 原生 SQL
 * 调用 PostGIS 空间函数实现读写，避免驱动层的类型绑定复杂度：
 * <ul>
 *     <li>读取：{@code ST_AsText(geom)} 输出 WKT、{@code ST_AsGeoJSON(geom)} 输出 GeoJSON</li>
 *     <li>写入：{@code ST_GeomFromText(#{wkt}, 4326)} 将 WKT 转为 geometry</li>
 *     <li>坐标分解：{@code ST_X(geom)} / {@code ST_Y(geom)} 提取经纬度</li>
 * </ul>
 *
 * <p>统一约定空间参考系为 EPSG:4326（WGS84 经纬度），与前端地图及
 * 种子数据 {@code ST_GeomFromText(..., 4326)} 保持一致。
 * 相关 SQL 见 resources/mapper/*.xml。
 */
@Configuration
public class PostGisConfig {

    /** 统一空间参考系：WGS84 经纬度 */
    public static final int SRID = 4326;

    /**
     * 构造 PostGIS 配置。
     */
    public PostGisConfig() {
        // 占位：空间函数经 SQL 调用，无需额外 Bean
    }
}
