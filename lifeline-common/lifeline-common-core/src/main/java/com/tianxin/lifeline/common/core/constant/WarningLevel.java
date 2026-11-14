package com.tianxin.lifeline.common.core.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 预警等级枚举。
 *
 * <p>四级预警分级：蓝 / 黄 / 橙 / 红，级别数值越小表示风险越高（1 为最高）。
 */
@Getter
@AllArgsConstructor
public enum WarningLevel {

    /** 蓝色预警（四级，一般风险） */
    BLUE(4, "BLUE", "蓝色预警", "#1E88E5"),
    /** 黄色预警（三级，较大风险） */
    YELLOW(3, "YELLOW", "黄色预警", "#FBC02D"),
    /** 橙色预警（二级，重大风险） */
    ORANGE(2, "ORANGE", "橙色预警", "#F57C00"),
    /** 红色预警（一级，特别重大风险） */
    RED(1, "RED", "红色预警", "#E53935");

    /** 预警级别（1 最高，4 最低） */
    private final int level;

    /** 等级编码 */
    private final String code;

    /** 等级描述 */
    private final String description;

    /** 展示色值 */
    private final String color;

    /**
     * 根据编码解析预警等级。
     *
     * @param code 等级编码
     * @return 匹配的预警等级，未匹配返回 null
     */
    public static WarningLevel fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (WarningLevel warningLevel : values()) {
            if (warningLevel.code.equalsIgnoreCase(code)) {
                return warningLevel;
            }
        }
        return null;
    }

    /**
     * 根据级别数值解析预警等级。
     *
     * @param level 级别数值（1-4）
     * @return 匹配的预警等级，未匹配返回 null
     */
    public static WarningLevel fromLevel(int level) {
        for (WarningLevel warningLevel : values()) {
            if (warningLevel.level == level) {
                return warningLevel;
            }
        }
        return null;
    }
}
