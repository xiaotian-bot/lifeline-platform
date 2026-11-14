package com.tianxin.lifeline.common.core.id;

/**
 * 雪花算法（Snowflake）分布式ID生成器（单例、线程安全）。
 *
 * <p>ID 结构（64bit）：1bit 符号位 + 41bit 毫秒时间戳 + 5bit 数据中心ID
 * + 5bit 工作机器ID + 12bit 同毫秒内序列号，单机每毫秒可产生 4096 个ID。
 */
public final class SnowflakeIdGenerator {

    /** 起始时间戳（2024-01-01 00:00:00 UTC） */
    private static final long EPOCH = 1704067200000L;

    /** 工作机器ID所占位数 */
    private static final long WORKER_ID_BITS = 5L;

    /** 数据中心ID所占位数 */
    private static final long DATA_CENTER_ID_BITS = 5L;

    /** 序列号所占位数 */
    private static final long SEQUENCE_BITS = 12L;

    /** 工作机器ID最大值（31） */
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

    /** 数据中心ID最大值（31） */
    private static final long MAX_DATA_CENTER_ID = ~(-1L << DATA_CENTER_ID_BITS);

    /** 序列号掩码（4095） */
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    /** 工作机器ID左移位数（12） */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    /** 数据中心ID左移位数（17） */
    private static final long DATA_CENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    /** 时间戳左移位数（22） */
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATA_CENTER_ID_BITS;

    /** 单例实例 */
    private static volatile SnowflakeIdGenerator instance;

    /** 工作机器ID */
    private final long workerId;

    /** 数据中心ID */
    private final long dataCenterId;

    /** 同毫秒内序列号 */
    private long sequence = 0L;

    /** 上一次生成ID的时间戳 */
    private long lastTimestamp = -1L;

    /**
     * 私有构造，默认使用 0 号数据中心与 0 号工作机器。
     */
    private SnowflakeIdGenerator() {
        this(0L, 0L);
    }

    /**
     * 私有构造。
     *
     * @param dataCenterId 数据中心ID（0-31）
     * @param workerId     工作机器ID（0-31）
     */
    private SnowflakeIdGenerator(long dataCenterId, long workerId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException("workerId 超出取值范围 [0, " + MAX_WORKER_ID + "]");
        }
        if (dataCenterId > MAX_DATA_CENTER_ID || dataCenterId < 0) {
            throw new IllegalArgumentException("dataCenterId 超出取值范围 [0, " + MAX_DATA_CENTER_ID + "]");
        }
        this.workerId = workerId;
        this.dataCenterId = dataCenterId;
    }

    /**
     * 获取单例实例（双重检查锁）。
     *
     * @return 雪花ID生成器单例
     */
    public static SnowflakeIdGenerator getInstance() {
        if (instance == null) {
            synchronized (SnowflakeIdGenerator.class) {
                if (instance == null) {
                    instance = new SnowflakeIdGenerator();
                }
            }
        }
        return instance;
    }

    /**
     * 生成下一个全局唯一ID。
     *
     * @return 64bit 唯一ID
     */
    public synchronized long nextId() {
        long timestamp = currentTimeMillis();
        if (timestamp < lastTimestamp) {
            throw new IllegalStateException("系统时钟回拨，拒绝生成ID，回拨毫秒数：" + (lastTimestamp - timestamp));
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                // 同毫秒内序列号耗尽，等待下一毫秒
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (dataCenterId << DATA_CENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * 生成下一个全局唯一ID的字符串形式。
     *
     * @return 唯一ID字符串
     */
    public String nextIdStr() {
        return String.valueOf(nextId());
    }

    /**
     * 阻塞直到进入下一毫秒。
     *
     * @param lastTimestamp 上一次时间戳
     * @return 下一毫秒时间戳
     */
    private long waitNextMillis(long lastTimestamp) {
        long timestamp = currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTimeMillis();
        }
        return timestamp;
    }

    /**
     * 获取当前毫秒时间戳。
     *
     * @return 当前毫秒时间戳
     */
    private long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
