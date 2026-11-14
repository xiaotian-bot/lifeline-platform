package com.tianxin.lifeline.warning.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tianxin.lifeline.common.data.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 预警等级配置实体（对应表 warning_level，蓝/黄/橙/红四级）。
 *
 * <p>种子数据：LEVEL_4 蓝（3001）/ LEVEL_3 黄（3002）/ LEVEL_2 橙（3003）/
 * LEVEL_1 红（3004），level_code 中的数字与 {@link com.tianxin.lifeline.common.core.constant.WarningLevel}
 * 枚举的级别数值一一对应，push_scope 为逗号分隔的推送对象，
 * response_require 为该等级的响应要求文案。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("warning_level")
public class WarningLevelEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 等级编码：LEVEL_1 / LEVEL_2 / LEVEL_3 / LEVEL_4 */
    private String levelCode;

    /** 等级名称：蓝色预警 / 黄色预警 / 橙色预警 / 红色预警 */
    private String levelName;

    /** 展示色值 */
    private String color;

    /** 响应要求文案 */
    private String responseRequire;

    /** 推送对象范围（逗号分隔，如：运维班组,运维主管） */
    private String pushScope;
}
