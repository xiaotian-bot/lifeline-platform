package com.tianxin.lifeline.operation.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tianxin.lifeline.common.data.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 运维物资实体（对应表 operation_material）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("operation_material")
public class OperationMaterial extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 物资名称 */
    private String materialName;

    /** 物资类型：valve 阀门 / pipe 管材 / clamp 抢修件 / anticorr 防腐材料 / insulation 保温材料 */
    private String materialType;

    /** 库存数量 */
    private BigDecimal stock;

    /** 计量单位 */
    private String unit;

    /** 存放仓库 */
    private String warehouse;
}
