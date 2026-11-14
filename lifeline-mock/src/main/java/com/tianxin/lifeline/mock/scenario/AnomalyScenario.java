package com.tianxin.lifeline.mock.scenario;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 异常场景枚举。
 *
 * <p>定义 Mock 系统可注入的管网异常：
 * <ul>
 *     <li>{@link #PRESSURE_DROP} 压力骤降（模拟管道泄漏）</li>
 *     <li>{@link #GAS_LEAK} 燃气泄漏（leakage 陡升 + pressure 下降）</li>
 *     <li>{@link #TEMP_SPIKE} 温度异常飙升</li>
 *     <li>{@link #HEATING_LEAK} 供热泄漏（flow 骤降 + temperature 异常跌落）</li>
 *     <li>{@link #DRAINAGE_OVERFLOW} 排水溢流（flow/pressure 持续攀升）</li>
 *     <li>{@link #THIRD_PARTY_DAMAGE} 第三方破坏（vibration 突变）</li>
 * </ul>
 *
 * <p>每个场景在持续时间内按进度（0~1）演进，进度由
 * {@code 已持续时间 / 总时长} 计算得出。
 */
@Getter
@AllArgsConstructor
public enum AnomalyScenario {

    /** 压力骤降：模拟管道破裂泄漏，压力快速跌落并维持低位 */
    PRESSURE_DROP("PRESSURE_DROP", "压力骤降（管道泄漏）", 120),

    /** 燃气泄漏：leakage 浓度陡升并伴随压力下降 */
    GAS_LEAK("GAS_LEAK", "燃气泄漏", 90),

    /** 温度飙升：温度持续异常升高 */
    TEMP_SPIKE("TEMP_SPIKE", "温度异常飙升", 90),

    /** 供热泄漏：管网失水导致流量骤降、供水温度异常跌落 */
    HEATING_LEAK("HEATING_LEAK", "供热泄漏", 120),

    /** 排水溢流：管内液位/流量持续攀升直至溢流 */
    DRAINAGE_OVERFLOW("DRAINAGE_OVERFLOW", "排水溢流", 150),

    /** 第三方破坏：外部施工撞击导致振动幅值突变 */
    THIRD_PARTY_DAMAGE("THIRD_PARTY_DAMAGE", "第三方破坏", 60);

    /** 场景编码 */
    private final String code;

    /** 场景描述 */
    private final String description;

    /** 场景默认持续时长（秒） */
    private final int defaultDurationSeconds;

    /**
     * 根据编码解析异常场景（忽略大小写）。
     *
     * @param code 场景编码
     * @return 匹配的异常场景，未匹配返回 null
     */
    public static AnomalyScenario fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (AnomalyScenario scenario : values()) {
            if (scenario.code.equalsIgnoreCase(code)) {
                return scenario;
            }
        }
        return null;
    }
}
