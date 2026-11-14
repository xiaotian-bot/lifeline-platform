package com.tianxin.lifeline.twin.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 数字孪生聚合服务异步与缓存配置。
 *
 * <p>两个独立线程池职责隔离：
 * <ul>
 *     <li>twinFeignExecutor：跨服务并行聚合调用（CompletableFuture 编排），
 *         避免单依赖慢拖垮 Tomcat 工作线程；</li>
 *     <li>twinSimulationExecutor：仿真推演异步执行，与聚合链路互不挤占。</li>
 * </ul>
 * 拒绝策略均采用 CallerRunsPolicy，过载时退化为调用方线程执行而非丢弃请求。
 *
 * <p>Caffeine L1 缓存用于模型数据聚合结果（30 秒），削减对 pipe-asset 的重复调用。
 */
@Configuration
public class TwinAsyncConfig {

    /** 模型数据 L1 缓存 Bean 名称 */
    public static final String MODEL_DATA_CACHE = "modelDataCache";

    /** 模型数据 L1 缓存过期时间（秒） */
    private static final long MODEL_DATA_TTL_SECONDS = 30L;

    /**
     * 跨服务并行聚合线程池。
     *
     * @return Feign 聚合编排执行器
     */
    @Bean("twinFeignExecutor")
    public Executor twinFeignExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(32);
        executor.setQueueCapacity(256);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("twin-feign-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }

    /**
     * 仿真推演异步线程池（轻量规则推演，CPU 开销低）。
     *
     * @return 仿真推演执行器
     */
    @Bean("twinSimulationExecutor")
    public Executor twinSimulationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(128);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("twin-sim-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }

    /**
     * 模型数据 Caffeine L1 缓存（写入后 30 秒过期，最多 1000 条）。
     *
     * @return 缓存实例（键为 pipeType/areaId 组合，值为聚合 VO）
     */
    @Bean(MODEL_DATA_CACHE)
    public Cache<String, Object> modelDataCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(MODEL_DATA_TTL_SECONDS))
                .maximumSize(1000)
                .build();
    }
}
