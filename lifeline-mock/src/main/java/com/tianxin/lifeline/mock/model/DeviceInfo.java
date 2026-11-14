package com.tianxin.lifeline.mock.model;

import com.tianxin.lifeline.common.core.constant.PipeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 模拟设备信息。
 *
 * <p>设备ID命名规范：{@code DEV-{类型}-{三位序号}}，与种子数据对齐：
 * DEV-GAS-001~002、DEV-WAT-001~002、DEV-DRN-001~002、
 * DEV-HTG-001~002、DEV-STR-001~002。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceInfo {

    /**
     * 设备短码到平台管网类型的映射。
     *
     * <p>GAS=燃气，WAT=供水，DRN=污水，HTG=供暖，STR=危废。
     */
    public static final Map<String, PipeType> CODE_TO_PIPE_TYPE = Map.of(
            "GAS", PipeType.GAS,
            "WAT", PipeType.WATER,
            "DRN", PipeType.SEWAGE,
            "HTG", PipeType.HEAT,
            "STR", PipeType.HAZARD);

    /** 设备ID，如 DEV-GAS-001 */
    private String deviceId;

    /** 所属管网类型 */
    private PipeType pipeType;

    /** 主传感器类型：pressure / temperature / flow / vibration / leakage */
    private String sensorType;

    /** 所属区域ID，如 AREA-001 */
    private String areaId;

    /** 是否为种子设备（种子数据中的 10 台） */
    private boolean seed;

    /**
     * 构建设备ID：DEV-{类型}-{三位序号}。
     *
     * @param typeCode 类型短码（GAS/WAT/DRN/HTG/STR）
     * @param seq      序号（从 1 开始）
     * @return 设备ID
     */
    public static String buildDeviceId(String typeCode, int seq) {
        return String.format("DEV-%s-%03d", typeCode, seq);
    }

    /**
     * 根据设备ID解析管网类型。
     *
     * @param deviceId 设备ID，如 DEV-GAS-001
     * @return 管网类型，无法识别时返回 null
     */
    public static PipeType parsePipeType(String deviceId) {
        if (deviceId == null) {
            return null;
        }
        String[] parts = deviceId.split("-");
        if (parts.length < 2) {
            return null;
        }
        return CODE_TO_PIPE_TYPE.get(parts[1].toUpperCase());
    }

    /**
     * 计算 TDengine 子表名：d_{device_code去横线}，如 d_DEVGAS001。
     *
     * @return 子表名
     */
    public String tdengineChildTable() {
        return "d_" + deviceId.replace("-", "");
    }
}
