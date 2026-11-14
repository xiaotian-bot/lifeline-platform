package com.tianxin.lifeline.warning.util;

/**
 * 设备/区域标识解析工具。
 *
 * <p>Mock 上报的 deviceId/areaId 为字符串编码（如 DEV-GAS-001 / AREA-001），
 * 而 warning_event 表的 device_id/area_id 为 bigint。
 * 本工具提取编码末尾的数字段作为数值ID；无法解析时退化为
 * 字符串哈希的正值，保证规则判定链路不因标识格式中断。
 */
public final class DeviceFieldUtils {

    private DeviceFieldUtils() {
    }

    /**
     * 将字符串标识解析为数值ID。
     *
     * <p>优先解析纯数字；其次提取末尾连续数字段（DEV-GAS-001 → 1）；
     * 均失败时取字符串哈希绝对值兜底。
     *
     * @param code 字符串标识
     * @return 数值ID，入参空白返回 null
     */
    public static Long parseId(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String trimmed = code.trim();
        // 纯数字直接解析
        if (trimmed.matches("\\d+")) {
            try {
                return Long.parseLong(trimmed);
            } catch (NumberFormatException ignored) {
                // 超出 long 范围走兜底
            }
        }
        // 提取末尾连续数字段
        int end = trimmed.length();
        int start = end;
        while (start > 0 && Character.isDigit(trimmed.charAt(start - 1))) {
            start--;
        }
        if (start < end) {
            try {
                return Long.parseLong(trimmed.substring(start, end));
            } catch (NumberFormatException ignored) {
                // 数字段超出 long 范围走兜底
            }
        }
        // 兜底：哈希取正
        return (long) Math.abs(trimmed.hashCode());
    }
}
