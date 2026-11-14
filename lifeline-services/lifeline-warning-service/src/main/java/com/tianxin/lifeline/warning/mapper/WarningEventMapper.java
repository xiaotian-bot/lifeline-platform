package com.tianxin.lifeline.warning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianxin.lifeline.warning.entity.WarningEvent;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * 预警事件 Mapper。
 *
 * <p>基础 CRUD 由 MyBatis-Plus BaseMapper 提供（含逻辑删除与自动填充），
 * 多维统计分组查询定义在 mapper/WarningEventMapper.xml 中。
 */
@Mapper
public interface WarningEventMapper extends BaseMapper<WarningEvent> {

    /**
     * 按预警级别分组统计数量。
     *
     * @return 分组结果列表，每项含 groupKey（级别数值）与 cnt（数量）
     */
    List<Map<String, Object>> countGroupByLevel();

    /**
     * 按管网类型分组统计数量。
     *
     * @return 分组结果列表，每项含 groupKey（管网类型）与 cnt（数量）
     */
    List<Map<String, Object>> countGroupByPipeType();

    /**
     * 按预警状态分组统计数量。
     *
     * @return 分组结果列表，每项含 groupKey（状态数值）与 cnt（数量）
     */
    List<Map<String, Object>> countGroupByStatus();
}
