package com.tianxin.lifeline.aigateway.service;

import com.tianxin.lifeline.api.ai.dto.ForecastRequestDTO;
import com.tianxin.lifeline.api.ai.dto.ForecastResultDTO;
import com.tianxin.lifeline.common.core.id.SnowflakeIdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * forecast_report 落库服务（已决策：由 ai-gateway-service 直写 PG）。
 *
 * <p>仅对「非降级」的预报结果落库；写入失败仅 warn 不影响接口响应。
 * JDBC 为阻塞操作，统一调度到 boundedElastic 线程池执行，避免阻塞
 * Netty 事件循环。
 */
@Slf4j
@Service
public class ForecastReportPersister {

    /** 设备编码末段数字提取（如 DEV-GAS-001 → 001） */
    private static final Pattern TRAILING_NUMBER = Pattern.compile("(\\d+)$");

    /** 兜底时间格式：yyyy-MM-dd HH:mm:ss */
    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;

    /**
     * 构造落库服务。
     *
     * @param jdbcTemplate JDBC 模板（Boot 自动装配，PG 数据源）
     */
    public ForecastReportPersister(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 异步落库预报报告（fire-and-forget，失败仅 warn）。
     *
     * @param request 预报请求（用于解析 device_id）
     * @param result  非降级的预报结果
     */
    public void persistAsync(ForecastRequestDTO request, ForecastResultDTO result) {
        Mono.fromRunnable(() -> doPersist(request, result))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(unused -> { },
                        e -> log.warn("forecast_report 落库失败（不影响响应）: {}", e.getMessage()));
    }

    /**
     * 执行落库。
     *
     * @param request 预报请求
     * @param result  预报结果
     */
    private void doPersist(ForecastRequestDTO request, ForecastResultDTO result) {
        long id = SnowflakeIdGenerator.getInstance().nextId();
        Long deviceId = parseDeviceId(request == null ? null : request.getDeviceCode());
        Timestamp riskTime = parseRiskTime(result.getRiskTime());
        BigDecimal confidence = result.getConfidence() == null
                ? null : BigDecimal.valueOf(result.getConfidence());

        jdbcTemplate.update(
                "INSERT INTO forecast_report (id, device_id, pipe_id, forecast_type, trend_desc, "
                        + "risk_time, confidence, report_content, created_at, updated_at, deleted) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)",
                id,
                deviceId,
                // pipe_id 可空：预报请求不含管段ID，留空由后续资产关联补齐
                null,
                result.getForecastType(),
                truncate(result.getTrendDesc(), 255),
                riskTime,
                confidence,
                result.getReportContent());
        log.info("forecast_report 落库成功: id={}, deviceId={}, forecastType={}",
                id, deviceId, result.getForecastType());
    }

    /**
     * 解析设备编码为设备ID。
     *
     * <p>规则：deviceCode 形如 DEV-GAS-001，按末段数字解析（001 → 1）；
     * 为空、无数字末段或数值溢出时置 null（device_id 列可空），
     * 不阻塞预报结果返回。
     *
     * @param deviceCode 设备编码
     * @return 设备ID，解析失败返回 null
     */
    private Long parseDeviceId(String deviceCode) {
        if (deviceCode == null || deviceCode.isBlank()) {
            return null;
        }
        Matcher matcher = TRAILING_NUMBER.matcher(deviceCode.trim());
        if (!matcher.find()) {
            // 编码末段非数字（如 DEV-GAS-A），无法映射为 bigint 设备ID
            return null;
        }
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException e) {
            // 数字超出 bigint 范围，置空
            return null;
        }
    }

    /**
     * 宽容解析风险时间为 timestamp。
     *
     * <p>依次尝试：毫秒时间戳 → ISO-8601（含 T 分隔）→ yyyy-MM-dd HH:mm:ss；
     * 均失败返回 null（risk_time 列可空）。
     *
     * @param riskTime 上游返回的风险时间字符串
     * @return SQL 时间戳，解析失败返回 null
     */
    private Timestamp parseRiskTime(String riskTime) {
        if (riskTime == null || riskTime.isBlank()) {
            return null;
        }
        String text = riskTime.trim();
        // 1) 毫秒时间戳
        if (text.matches("^\\d{10,13}$")) {
            try {
                long millis = Long.parseLong(text);
                if (text.length() == 10) {
                    millis *= 1000L;
                }
                return new Timestamp(millis);
            } catch (NumberFormatException ignored) {
                // 继续尝试文本格式
            }
        }
        // 2) ISO-8601，如 2026-08-20T12:00:00
        try {
            return Timestamp.valueOf(LocalDateTime.parse(text));
        } catch (DateTimeParseException ignored) {
            // 继续尝试下一种格式
        }
        // 3) 常见空格分隔格式
        try {
            return Timestamp.valueOf(LocalDateTime.parse(text, DATETIME_FORMATTER));
        } catch (DateTimeParseException e) {
            log.warn("forecast_report.risk_time 无法解析, 置空: {}", text);
            return null;
        }
    }

    /**
     * 截断字符串以适配列长。
     *
     * @param text      原文
     * @param maxLength 最大长度
     * @return 截断后字符串
     */
    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }
}
