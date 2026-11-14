package com.tianxin.lifeline.common.data.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据库实体基类。
 *
 * <p>全平台所有业务表必须包含以下四个基础字段：
 * <ul>
 *     <li>id：雪花算法主键</li>
 *     <li>created_at：创建时间（插入自动填充）</li>
 *     <li>updated_at：更新时间（插入/更新自动填充）</li>
 *     <li>deleted：逻辑删除标记（0 未删除 / 1 已删除）</li>
 * </ul>
 */
@Data
public class BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 未删除标记 */
    public static final int NOT_DELETED = 0;

    /** 已删除标记 */
    public static final int DELETED = 1;

    /** 主键ID（雪花算法） */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 逻辑删除标记：0 未删除 / 1 已删除 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
