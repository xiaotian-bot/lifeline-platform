package com.tianxin.lifeline.monitor.ws;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * WebSocket 实时推送配置属性（P10-3）。
 *
 * <p>配置前缀 {@code lifeline.monitor.ws}，示例：
 * <pre>
 * lifeline:
 *   monitor:
 *     ws:
 *       enabled: true
 *       throttle-per-device-ms: 1000
 *       max-sessions-per-area: 200
 *       max-total-sessions: 2000
 * </pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "lifeline.monitor.ws")
public class MonitorWebSocketProperties {

    /** 是否启用 WebSocket 实时推送（关闭后拒绝连接且写入侧不发布消息），默认启用 */
    private boolean enabled = true;

    /** 同区域同设备广播节流间隔（毫秒）：每设备最多每 interval 一条，默认 1000（每秒 1 条） */
    private long throttlePerDeviceMs = 1000L;

    /** 单区域（areaId）会话数上限，超限拒绝新连接（防单区域连接数打爆），默认 200 */
    private int maxSessionsPerArea = 200;

    /** 全服务 WebSocket 会话总数上限，超限拒绝新连接（连接数保护），默认 2000 */
    private int maxTotalSessions = 2000;
}
