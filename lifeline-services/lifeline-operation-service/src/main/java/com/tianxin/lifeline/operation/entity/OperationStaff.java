package com.tianxin.lifeline.operation.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tianxin.lifeline.common.data.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 运维人员实体（对应表 operation_staff）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("operation_staff")
public class OperationStaff extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 人员编码 */
    private String staffCode;

    /** 人员姓名 */
    private String staffName;

    /** 技能类型：gas 燃气 / water 供水 / heating 供热 / drainage 排水 / composite 综合 */
    private String skillType;

    /** 联系电话 */
    private String phone;

    /** 人员状态：1 在岗 / 0 停用 */
    private Integer status;

    /** 所属部门ID */
    private Long deptId;
}
