package com.tianxin.lifeline.asset.dto;

import com.tianxin.lifeline.api.asset.dto.DeviceDTO;
import com.tianxin.lifeline.api.asset.dto.PipeDTO;
import com.tianxin.lifeline.asset.entity.Manhole;
import com.tianxin.lifeline.asset.entity.PipeAttribute;
import com.tianxin.lifeline.asset.entity.PipeTopology;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 一管一档聚合响应。
 *
 * <p>聚合单条管网的主档、扩展属性、挂载设备、空间关联井室与拓扑关系。
 */
@Data
public class PipeArchiveResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 管网主档（pipe_main） */
    private PipeDTO pipe;

    /** 扩展属性列表（pipe_attribute，KV 结构） */
    private List<PipeAttribute> attributes = new ArrayList<>();

    /** 挂载设备列表（pipe_device） */
    private List<DeviceDTO> devices = new ArrayList<>();

    /** 空间关联井室列表（按邻近半径聚合，manhole） */
    private List<Manhole> manholes = new ArrayList<>();

    /** 关联拓扑关系列表（pipe_topology） */
    private List<PipeTopology> topologies = new ArrayList<>();
}
