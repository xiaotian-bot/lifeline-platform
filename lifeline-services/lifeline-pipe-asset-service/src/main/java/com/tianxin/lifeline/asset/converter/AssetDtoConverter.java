package com.tianxin.lifeline.asset.converter;

import com.tianxin.lifeline.api.asset.dto.DeviceDTO;
import com.tianxin.lifeline.api.asset.dto.PipeDTO;
import com.tianxin.lifeline.asset.entity.PipeDevice;
import com.tianxin.lifeline.asset.entity.PipeMain;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 管网资产实体到服务间共享 DTO 的转换器。
 *
 * <p>字段映射约定：
 * <ul>
 *     <li>{@code depth} → {@code buriedDepth}（字段更名）</li>
 *     <li>{@code installDate(LocalDate)} → {@code commissionDate(LocalDateTime)}（当日零点）</li>
 *     <li>{@code status(Integer 0/1)} → {@code status(String 停用/运行 或 离线/在线)}</li>
 *     <li>{@code DeviceDTO.sensorType} 由 {@code deviceType} 直接映射</li>
 * </ul>
 *
 * <p>已决策口径 1 —— pipe_type 双编码体系：
 * DB 存量保持小写（gas/water/drainage/heating/storm），PipeDTO 输出原样透传；
 * 接口入参大小写不敏感，经 {@link #normalizePipeType(String)} 归一；
 * {@code PipeType} 枚举命中失败的编码（如 drainage/storm/heating/hazard）原样透传、不阻断请求。
 *
 * <p>已决策口径 2 —— area_id 缺失：
 * DB 中暂无 area_id/pipe_level/protocol 对应列，{@code PipeDTO.areaId}、
 * {@code PipeDTO.pipeLevel}、{@code DeviceDTO.areaId}、{@code DeviceDTO.protocol}
 * 暂返回 null，待区域划分表落地后补齐。
 */
public final class AssetDtoConverter {

    /**
     * 私有构造，工具类禁止实例化。
     */
    private AssetDtoConverter() {
    }

    /**
     * 管网实体转 PipeDTO（Feign 契约对象）。
     *
     * @param pipe 管网实体（含 WKT）
     * @return 管网 DTO，入参为 null 返回 null
     */
    public static PipeDTO toPipeDto(PipeMain pipe) {
        if (pipe == null) {
            return null;
        }
        PipeDTO dto = new PipeDTO();
        dto.setPipeId(pipe.getId());
        dto.setPipeCode(pipe.getPipeCode());
        dto.setPipeName(pipe.getPipeName());
        // 双编码体系：DB 小写编码原样透传，不做大写枚举强转，避免 drainage/storm/heating 丢失
        dto.setPipeType(pipe.getPipeType());
        dto.setDiameter(pipe.getDiameter() == null ? null : pipe.getDiameter().doubleValue());
        dto.setMaterial(pipe.getMaterial());
        dto.setLength(pipe.getLength() == null ? null : pipe.getLength().doubleValue());
        // 字段更名：depth → buriedDepth
        dto.setBuriedDepth(pipe.getDepth() == null ? null : pipe.getDepth().doubleValue());
        // 决策口径 2：area_id 暂无对应列，置 null
        dto.setAreaId(null);
        // 决策口径 2：pipe_level 暂无对应列，置 null
        dto.setPipeLevel(null);
        // installDate(LocalDate) → commissionDate(LocalDateTime)，取当日零点
        dto.setCommissionDate(pipe.getInstallDate() == null
                ? null : LocalDateTime.of(pipe.getInstallDate(), LocalTime.MIN));
        // Integer 0/1 → 中文资产状态文本
        dto.setStatus(pipeStatusText(pipe.getStatus()));
        dto.setGeometryWkt(pipe.getGeomWkt());
        return dto;
    }

    /**
     * 设备实体转 DeviceDTO（Feign 契约对象）。
     *
     * <p>实体的 {@code pipeType} 由 SQL JOIN pipe_main 回填。
     *
     * @param device 设备实体（含经纬度与管网类型）
     * @return 设备 DTO，入参为 null 返回 null
     */
    public static DeviceDTO toDeviceDto(PipeDevice device) {
        if (device == null) {
            return null;
        }
        DeviceDTO dto = new DeviceDTO();
        dto.setDeviceId(device.getId());
        dto.setDeviceCode(device.getDeviceCode());
        dto.setDeviceName(device.getDeviceName());
        dto.setDeviceType(device.getDeviceType());
        // sensorType 由 deviceType 直接映射（pressure/temperature/flow/vibration/corrosion/leakage 等）
        dto.setSensorType(device.getDeviceType());
        dto.setPipeId(device.getPipeId());
        // 管网类型经 JOIN pipe_main 回填，原样透传小写编码
        dto.setPipeType(device.getPipeType());
        // 决策口径 2：area_id 暂无对应列，置 null
        dto.setAreaId(null);
        dto.setLongitude(device.getLongitude());
        dto.setLatitude(device.getLatitude());
        // 决策口径 2：protocol 暂无对应列，置 null
        dto.setProtocol(null);
        // 设备状态直接透传 Integer（0=离线 / 1=在线 / 2=故障），前端按数值映射显示文案
        dto.setStatus(device.getStatus());
        dto.setInstallDate(device.getInstallDate() == null
                ? null : LocalDateTime.of(device.getInstallDate(), LocalTime.MIN));
        return dto;
    }

    /**
     * 管网类型入参归一化（大小写不敏感 + 同义词映射到 DB 小写编码）。
     *
     * <p>已决策口径 1：DB 存量为小写；PipeType 枚举命中失败的编码
     * （drainage/storm/heating/hazard 等）原样透传、不阻断请求。
     * 同义词映射兼容 Feign 契约的大写编码（HEAT→heating、SEWAGE→drainage）。
     *
     * @param pipeType 原始入参（可空）
     * @return 归一化后的管网类型编码，空白返回 null
     */
    public static String normalizePipeType(String pipeType) {
        if (pipeType == null || pipeType.isBlank()) {
            return null;
        }
        String lower = pipeType.trim().toLowerCase();
        return switch (lower) {
            // Feign 契约大写编码与 DB 小写编码的同义词对齐
            case "heat", "heating" -> "heating";
            case "sewage", "drainage" -> "drainage";
            case "water" -> "water";
            case "gas" -> "gas";
            case "storm" -> "storm";
            // 枚举命中失败的编码原样透传（如 hazard），不阻断业务
            default -> lower;
        };
    }

    /**
     * 管网状态码转文本。
     *
     * @param status 状态码：0 停用 / 1 运行
     * @return 状态文本
     */
    public static String pipeStatusText(Integer status) {
        return status != null && status == 1 ? "运行" : "停用";
    }

    /**
     * 设备状态码转文本。
     *
     * @param status 状态码：0 离线 / 1 在线
     * @return 状态文本
     */
    public static String deviceStatusText(Integer status) {
        return status != null && status == 1 ? "在线" : "离线";
    }
}
