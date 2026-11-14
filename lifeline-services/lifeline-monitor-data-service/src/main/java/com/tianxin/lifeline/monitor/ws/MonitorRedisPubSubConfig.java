package com.tianxin.lifeline.monitor.ws;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Redis Pub/Sub 订阅容器配置（实时推送扇入侧）。
 *
 * <p>以模式 {@code monitor:rt:*} 订阅全部区域频道。服务无状态：
 * 多副本各自订阅同一模式，Redis 向所有订阅者投递消息，每个副本仅
 * 广播给挂在自身内存中的会话，天然水平扩展无需额外协调。
 */
@Configuration
public class MonitorRedisPubSubConfig {

    /** 订阅频道模式（覆盖全部区域：monitor:rt:{areaId}） */
    private static final String CHANNEL_PATTERN = MonitorRealtimePublisher.CHANNEL_PREFIX + "*";

    /**
     * 实时推送订阅容器。
     *
     * @param connectionFactory Redis 连接工厂
     * @param broadcastListener 广播监听器
     * @return RedisMessageListenerContainer
     */
    @Bean
    public RedisMessageListenerContainer monitorRealtimeListenerContainer(
            RedisConnectionFactory connectionFactory,
            MonitorRealtimeBroadcastListener broadcastListener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(broadcastListener, new PatternTopic(CHANNEL_PATTERN));
        return container;
    }
}
