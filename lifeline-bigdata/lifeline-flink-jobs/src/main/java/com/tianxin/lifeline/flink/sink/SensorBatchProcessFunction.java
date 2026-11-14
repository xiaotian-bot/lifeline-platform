package com.tianxin.lifeline.flink.sink;

import com.tianxin.lifeline.flink.common.SensorMessageParser;
import com.tianxin.lifeline.flink.model.SensorMessage;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 攒批算子：keyBy(deviceId) 之后按「2000 条 / 500ms 先到先触发」聚合。
 *
 * <p>实现要点：
 * <ul>
 *     <li>缓冲使用 Flink 托管 ListState（RocksDB 后端），Checkpoint 随快照持久化，
 *         故障恢复不丢已攒数据；</li>
 *     <li>ProcessingTime Timer 兜底低流量场景的 500ms 延迟上限；</li>
 *     <li>输出 List&lt;SensorMessage&gt; 批，由下游 TDengineBatchSink 拼多子表 INSERT；
 *         Sink 端 AT_LEAST_ONCE + TDengine 同 ts 幂等覆盖写入，
 *         重复投递不产生脏数据。</li>
 * </ul>
 */
public class SensorBatchProcessFunction
        extends KeyedProcessFunction<String, String, List<SensorMessage>>
        implements CheckpointedFunction {

    private static final Logger log = LoggerFactory.getLogger(SensorBatchProcessFunction.class);

    /** 攒批条数阈值 */
    private final int batchSize;

    /** 定时冲刷间隔（毫秒） */
    private final long batchIntervalMs;

    /** 攒批缓冲（Flink 托管状态） */
    private transient ListState<SensorMessage> bufferState;

    /** 当前已注册的冲刷定时器时间戳（null 表示无定时器） */
    private transient ValueState<Long> timerTsState;

    /** 当前缓冲条数（运行时计数，避免频繁迭代 ListState） */
    private transient int currentCount;

    /** 当前分区键（仅用于调试日志，来自 processElement 上下文） */
    private transient String currentKey;

    /**
     * 构造攒批算子。
     *
     * @param batchSize       条数阈值（2000）
     * @param batchIntervalMs 定时冲刷间隔毫秒（500）
     */
    public SensorBatchProcessFunction(int batchSize, long batchIntervalMs) {
        this.batchSize = batchSize;
        this.batchIntervalMs = batchIntervalMs;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        timerTsState = getRuntimeContext().getState(new ValueStateDescriptor<>("batch-timer", Long.class));
        currentCount = 0;
        if (bufferState != null) {
            for (SensorMessage ignored : bufferState.get()) {
                currentCount++;
            }
        }
    }

    @Override
    public void initializeState(FunctionInitializationContext context) throws Exception {
        bufferState = context.getKeyedStateStore().getListState(
                new ListStateDescriptor<>("sensor-batch-buffer",
                        TypeInformation.of(new TypeHint<SensorMessage>() {
                        })));
    }

    @Override
    public void processElement(String payload, Context ctx, Collector<List<SensorMessage>> out)
            throws Exception {
        currentKey = ctx.getCurrentKey();
        for (SensorMessage message : SensorMessageParser.parse(payload)) {
            if (!SensorMessageParser.isValid(message)) {
                // 理论上 clean topic 均为合法数据，此处为防御性校验
                continue;
            }
            bufferState.add(message);
            currentCount++;
        }
        if (currentCount >= batchSize) {
            flush(out);
            clearTimer(ctx);
            return;
        }
        // 未达阈值：注册 500ms 兜底定时器（已有定时器则不重复注册）
        if (timerTsState.value() == null) {
            long fireTs = ctx.timerService().currentProcessingTime() + batchIntervalMs;
            ctx.timerService().registerProcessingTimeTimer(fireTs);
            timerTsState.update(fireTs);
        }
    }

    @Override
    public void onTimer(long timestamp, OnTimerContext ctx, Collector<List<SensorMessage>> out)
            throws Exception {
        Long registered = timerTsState.value();
        if (registered != null && registered == timestamp) {
            flush(out);
            timerTsState.clear();
        }
    }

    /**
     * Checkpoint 快照：缓冲本身即托管 ListState，随快照一并持久化，
     * 故障恢复后从断点继续攒批，不丢已接收未冲刷的数据。
     */
    @Override
    public void snapshotState(FunctionSnapshotContext context) {
        // 无额外非托管状态需要同步
    }

    /**
     * 冲刷缓冲：整批输出并清空状态。
     *
     * @param out 输出收集器
     */
    private void flush(Collector<List<SensorMessage>> out) throws Exception {
        if (currentCount == 0) {
            return;
        }
        List<SensorMessage> drained = new ArrayList<>(currentCount);
        for (SensorMessage message : bufferState.get()) {
            drained.add(message);
        }
        bufferState.clear();
        currentCount = 0;
        if (!drained.isEmpty()) {
            out.collect(drained);
            if (log.isDebugEnabled()) {
                log.debug("攒批冲刷: key={}, {} 条", currentKey, drained.size());
            }
        }
    }

    /**
     * 清除未到期的定时器（条数阈值触发冲刷后不再需要兜底定时器）。
     *
     * @param ctx 算子上下文
     */
    private void clearTimer(Context ctx) throws Exception {
        Long registered = timerTsState.value();
        if (registered != null) {
            ctx.timerService().deleteProcessingTimeTimer(registered);
            timerTsState.clear();
        }
    }
}
