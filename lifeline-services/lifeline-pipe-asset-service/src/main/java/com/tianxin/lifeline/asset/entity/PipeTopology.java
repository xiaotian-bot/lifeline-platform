package com.tianxin.lifeline.asset.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tianxin.lifeline.common.data.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 管网拓扑关系实体（对应表 pipe_topology）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pipe_topology")
public class PipeTopology extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 起始管网ID */
    private Long fromPipeId;

    /** 目标管网ID */
    private Long toPipeId;

    /** 起始节点（井室/设备编码） */
    private String fromNode;

    /** 目标节点（井室/设备编码） */
    private String toNode;

    /** 关系类型（如 connected/branch/valve） */
    private String relationType;
}
