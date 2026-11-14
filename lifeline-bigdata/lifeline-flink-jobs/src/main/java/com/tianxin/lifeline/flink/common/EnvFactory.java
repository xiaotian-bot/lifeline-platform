package com.tianxin.lifeline.flink.common;

import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.contrib.streaming.state.EmbeddedRocksDBStateBackend;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.concurrent.TimeUnit;

/**
 * Flink 执行环境统一工厂。
 *
 * <p>四个作业统一的环境基线：
 * <ul>
 *     <li>Checkpoint：30 秒一次，EXACTLY_ONCE 语义；</li>
 *     <li>状态后端：RocksDB（大状态场景，去重/攒批/抑制窗口状态均落 RocksDB）；</li>
 *     <li>重启策略：固定延迟 3 次，间隔 10 秒；</li>
 *     <li>并行度：由 {@link JobParams#parallelism()} 参数化（默认 4，可配 16）。</li>
 * </ul>
 */
public final class EnvFactory {

    /** Checkpoint 间隔（毫秒） */
    public static final long CHECKPOINT_INTERVAL_MS = 30_000L;

    /** Checkpoint 之间最小间隔（毫秒） */
    public static final long MIN_PAUSE_BETWEEN_CHECKPOINTS_MS = 10_000L;

    /** Checkpoint 超时时间（毫秒） */
    public static final long CHECKPOINT_TIMEOUT_MS = 120_000L;

    private EnvFactory() {
    }

    /**
     * 创建统一配置的流执行环境。
     *
     * @param params 作业启动参数
     * @return 已配置的 StreamExecutionEnvironment
     */
    public static StreamExecutionEnvironment create(JobParams params) {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(params.parallelism());

        // RocksDB 状态后端（去重状态 / 攒批缓冲 / 抑制窗口状态均可能随设备数增长）
        env.setStateBackend(new EmbeddedRocksDBStateBackend());

        // Checkpoint：30s EXACTLY_ONCE
        env.enableCheckpointing(CHECKPOINT_INTERVAL_MS, CheckpointingMode.EXACTLY_ONCE);
        CheckpointConfig checkpointConfig = env.getCheckpointConfig();
        checkpointConfig.setCheckpointStorage(params.checkpointDir());
        checkpointConfig.setMinPauseBetweenCheckpoints(MIN_PAUSE_BETWEEN_CHECKPOINTS_MS);
        checkpointConfig.setCheckpointTimeout(CHECKPOINT_TIMEOUT_MS);
        checkpointConfig.setMaxConcurrentCheckpoints(1);
        checkpointConfig.setTolerableCheckpointFailureNumber(3);
        // 作业取消时保留 Checkpoint，便于从断点恢复
        checkpointConfig.setExternalizedCheckpointCleanup(
                CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);

        // 固定延迟重启：3 次，间隔 10 秒
        env.setRestartStrategy(RestartStrategies.fixedDelayRestart(3, Time.of(10, TimeUnit.SECONDS)));
        return env;
    }
}
