package com.tianxin.lifeline.common.core.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 管网类型枚举。
 *
 * <p>安塞区城市安全生命线覆盖的五大类管网：
 * 供水 / 供暖 / 燃气 / 污水 / 危废。
 */
@Getter
@AllArgsConstructor
public enum PipeType {

    /** 供水管网 */
    WATER("WATER", "供水管网"),
    /** 供暖管网 */
    HEAT("HEAT", "供暖管网"),
    /** 燃气管网 */
    GAS("GAS", "燃气管网"),
    /** 污水管网 */
    SEWAGE("SEWAGE", "污水管网"),
    /** 危废管网 */
    HAZARD("HAZARD", "危废管网");

    /** 类型编码 */
    private final String code;

    /** 类型描述 */
    private final String description;

    /**
     * 根据编码解析管网类型。
     *
     * @param code 类型编码
     * @return 匹配的管网类型，未匹配返回 null
     */
    public static PipeType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (PipeType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }
}
