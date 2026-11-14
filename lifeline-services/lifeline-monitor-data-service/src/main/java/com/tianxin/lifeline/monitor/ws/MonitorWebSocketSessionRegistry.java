package com.tianxin.lifeline.monitor.ws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 监测实时推送 WebSocket 会话注册表。
 *
 * <p>按区域编码（areaId）维护 {@link CopyOnWriteArraySet} 会话集合，
 * 广播时遍历对应区域集合；会话在连接建立时注册、连接关闭/传输异常时移除，
 * 广播前二次校验 isOpen 兜底清理僵死会话。
 *
 * <p>本服务无状态：会话仅存在于当前副本内存，跨副本扇出依赖
 * Redis Pub/Sub（每个副本各自订阅，天然水平扩展）。
 */
@Slf4j
@Component
public class MonitorWebSocketSessionRegistry {

    /** areaId → 该区域订阅会话集合 */
    private final ConcurrentHashMap<String, CopyOnWriteArraySet<WebSocketSession>> sessionsByArea =
            new ConcurrentHashMap<>();

    /**
     * 注册会话到指定区域。
     *
     * @param areaId  区域编码（如 AREA-001）
     * @param session WebSocket 会话
     */
    public void register(String areaId, WebSocketSession session) {
        sessionsByArea.computeIfAbsent(areaId, k -> new CopyOnWriteArraySet<>()).add(session);
        log.debug("WebSocket 会话注册: areaId={}, sessionId={}, 当前区域会话数={}",
                areaId, session.getId(), sessionsByArea.get(areaId).size());
    }

    /**
     * 移除会话（连接关闭/异常时调用，幂等）。
     *
     * @param areaId  区域编码
     * @param session WebSocket 会话
     */
    public void remove(String areaId, WebSocketSession session) {
        Set<WebSocketSession> sessions = sessionsByArea.get(areaId);
        if (sessions != null) {
            sessions.remove(session);
        }
    }

    /**
     * 获取指定区域的会话集合（只读视图，可能为空集合）。
     *
     * @param areaId 区域编码
     * @return 会话集合
     */
    public Set<WebSocketSession> getSessions(String areaId) {
        Set<WebSocketSession> sessions = sessionsByArea.get(areaId);
        return sessions == null ? Collections.emptySet() : sessions;
    }

    /**
     * 统计指定区域当前会话数。
     *
     * @param areaId 区域编码
     * @return 会话数
     */
    public int countByArea(String areaId) {
        Set<WebSocketSession> sessions = sessionsByArea.get(areaId);
        return sessions == null ? 0 : sessions.size();
    }

    /**
     * 统计全服务当前会话总数。
     *
     * @return 会话总数
     */
    public int countTotal() {
        return sessionsByArea.values().stream().mapToInt(Set::size).sum();
    }
}
