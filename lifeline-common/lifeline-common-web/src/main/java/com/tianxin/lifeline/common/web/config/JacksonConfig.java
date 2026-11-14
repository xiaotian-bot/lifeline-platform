package com.tianxin.lifeline.common.web.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigInteger;
import java.util.TimeZone;

/**
 * Jackson 全局序列化配置。
 *
 * <ul>
 *     <li>日期时间统一格式化为 {@code yyyy-MM-dd HH:mm:ss}，时区 GMT+8</li>
 *     <li>Long / BigInteger 序列化为字符串，防止前端 JavaScript 精度丢失</li>
 * </ul>
 */
@Configuration
public class JacksonConfig {

    /** 全局日期时间格式 */
    public static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /** 全局日期格式 */
    public static final String DATE_PATTERN = "yyyy-MM-dd";

    /** 全局时间格式 */
    public static final String TIME_PATTERN = "HH:mm:ss";

    /** 时区：东八区 */
    public static final String TIME_ZONE = "GMT+8";

    /**
     * 定制全局 ObjectMapper 构建规则。
     *
     * @return Jackson 构建器定制器
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> builder
                .timeZone(TimeZone.getTimeZone(TIME_ZONE))
                .simpleDateFormat(DATETIME_PATTERN)
                .serializerByType(java.time.LocalDate.class, new com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer(
                        java.time.format.DateTimeFormatter.ofPattern(DATE_PATTERN)))
                .serializerByType(java.time.LocalDateTime.class, new com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer(
                        java.time.format.DateTimeFormatter.ofPattern(DATETIME_PATTERN)))
                .serializerByType(java.time.LocalTime.class, new com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer(
                        java.time.format.DateTimeFormatter.ofPattern(TIME_PATTERN)))
                .deserializerByType(java.time.LocalDate.class, new com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer(
                        java.time.format.DateTimeFormatter.ofPattern(DATE_PATTERN)))
                .deserializerByType(java.time.LocalDateTime.class, new com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer(
                        java.time.format.DateTimeFormatter.ofPattern(DATETIME_PATTERN)))
                .deserializerByType(java.time.LocalTime.class, new com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer(
                        java.time.format.DateTimeFormatter.ofPattern(TIME_PATTERN)))
                // Long 类型转 String，防止前端精度丢失
                .serializerByType(Long.class, ToStringSerializer.instance)
                .serializerByType(Long.TYPE, ToStringSerializer.instance)
                .serializerByType(BigInteger.class, ToStringSerializer.instance);
    }
}
