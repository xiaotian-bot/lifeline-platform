package com.tianxin.lifeline.twin.service;

import com.tianxin.lifeline.common.core.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

/**
 * Feign 调用降级支撑（聚合编排规范）。
 *
 * <p>单依赖失败一律降级为 null 并在降级来源列表登记，不向上抛异常，
 * 保证聚合接口永不因单点依赖失败而 5xx。
 */
@Slf4j
@Component
public class TwinFeignSupport {

    /**
     * 安全执行 Feign 调用，失败降级返回 null（成功但无数据也登记降级）。
     *
     * @param source          数据源标识（用于降级标记）
     * @param call            Feign 调用供应器
     * @param degradedSources 降级来源登记列表（失败时追加 source）
     * @param <T>             返回数据类型
     * @return 调用成功且 Result 成功时返回 data，否则 null
     */
    public <T> T callOrNull(String source, Supplier<Result<T>> call, List<String> degradedSources) {
        return callOrNull(source, call, degradedSources, true);
    }

    /**
     * 安全执行 Feign 调用，失败降级返回 null。
     *
     * <p>区分「调用失败」与「成功无数据」：仅调用异常或 Result 失败时登记降级；
     * Result 成功但 data 为 null（如资源不存在）不登记 degradedSources。
     *
     * @param source            数据源标识（用于降级标记）
     * @param call              Feign 调用供应器
     * @param degradedSources   降级来源登记列表（失败时追加 source）
     * @param degradeOnEmptyData 成功但 data 为 null 时是否登记降级
     * @param <T>               返回数据类型
     * @return 调用成功且 Result 成功时返回 data，否则 null
     */
    public <T> T callOrNull(String source, Supplier<Result<T>> call,
                            List<String> degradedSources, boolean degradeOnEmptyData) {
        try {
            Result<T> result = call.get();
            if (result != null && result.isSuccess()) {
                // 成功码：data 为 null 属「成功无数据」，按需登记降级
                if (result.getData() == null && degradeOnEmptyData) {
                    degradedSources.add(source);
                }
                return result.getData();
            }
            log.warn("Feign调用返回失败, 降级处理: source={}, result={}", source, result);
        } catch (Exception e) {
            log.warn("Feign调用异常, 降级处理: source={}", source, e);
        }
        degradedSources.add(source);
        return null;
    }
}
