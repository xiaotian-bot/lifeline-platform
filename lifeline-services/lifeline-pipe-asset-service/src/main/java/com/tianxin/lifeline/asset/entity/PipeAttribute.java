package com.tianxin.lifeline.asset.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tianxin.lifeline.common.data.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 管网扩展属性实体（对应表 pipe_attribute，KV 结构）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pipe_attribute")
public class PipeAttribute extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 所属管网ID */
    private Long pipeId;

    /** 属性键 */
    private String attrKey;

    /** 属性值 */
    private String attrValue;

    /** 属性单位 */
    private String attrUnit;
}
