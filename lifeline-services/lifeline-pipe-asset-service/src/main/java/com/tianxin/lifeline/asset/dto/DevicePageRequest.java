package com.tianxin.lifeline.asset.dto;

import com.tianxin.lifeline.common.core.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 设备分页查询请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DevicePageRequest extends PageQuery {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 设备类型：pressure/temp/flow/vibration/corrosion/leakage/video */
    private String deviceType;

    /** 所属管网ID */
    private Long pipeId;

    /** 状态：0 离线 / 1 在线 */
    private Integer status;

    /** 设备编码/名称关键字 */
    private String keyword;
}
