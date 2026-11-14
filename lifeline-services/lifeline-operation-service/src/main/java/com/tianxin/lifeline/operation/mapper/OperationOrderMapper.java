package com.tianxin.lifeline.operation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tianxin.lifeline.operation.entity.OperationOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 运维工单 Mapper。
 *
 * <p>基础 CRUD 由 MyBatis-Plus BaseMapper 提供（含逻辑删除与自动填充），
 * 分页查询 / 按预警事件查询 / 分组统计定义在 mapper/OperationOrderMapper.xml 中。
 */
@Mapper
public interface OperationOrderMapper extends BaseMapper<OperationOrder> {

    /**
     * 分页查询工单列表（多条件过滤，红色预警应急工单默认置顶）。
     *
     * @param status         状态数值（可空）
     * @param orderType      工单类型（可空）
     * @param warningEventId 预警事件ID（可空）
     * @param assignee       责任人标识（可空）
     * @param priority       优先级（可空）
     * @param startTime      创建时间起（可空，yyyy-MM-dd HH:mm:ss）
     * @param endTime        创建时间止（可空，yyyy-MM-dd HH:mm:ss）
     * @param orderBy        排序白名单键（可空，空则使用默认置顶排序）
     * @param offset         偏移量
     * @param size           每页条数
     * @return 当前页工单列表
     */
    List<OperationOrder> selectOrderPage(@Param("status") Integer status,
                                         @Param("orderType") String orderType,
                                         @Param("warningEventId") Long warningEventId,
                                         @Param("assignee") String assignee,
                                         @Param("priority") Integer priority,
                                         @Param("startTime") String startTime,
                                         @Param("endTime") String endTime,
                                         @Param("orderBy") String orderBy,
                                         @Param("offset") long offset,
                                         @Param("size") int size);

    /**
     * 统计与分页同条件的工单总数。
     *
     * @param status         状态数值（可空）
     * @param orderType      工单类型（可空）
     * @param warningEventId 预警事件ID（可空）
     * @param assignee       责任人标识（可空）
     * @param priority       优先级（可空）
     * @param startTime      创建时间起（可空）
     * @param endTime        创建时间止（可空）
     * @return 工单总数
     */
    long countOrder(@Param("status") Integer status,
                    @Param("orderType") String orderType,
                    @Param("warningEventId") Long warningEventId,
                    @Param("assignee") String assignee,
                    @Param("priority") Integer priority,
                    @Param("startTime") String startTime,
                    @Param("endTime") String endTime);

    /**
     * 按预警事件ID查询关联工单列表。
     *
     * @param warningEventId 预警事件ID
     * @return 关联工单列表（按创建时间降序）
     */
    List<OperationOrder> selectByWarningEventId(@Param("warningEventId") Long warningEventId);

    /**
     * 查询指定巡检计划派生的巡检工单（order_type=INSPECT，
     * content 携带 [inspectionPlanId=计划ID] 关联标记）。
     *
     * @param planId 巡检计划ID
     * @return 派生的巡检工单列表（按创建时间降序）
     */
    List<OperationOrder> selectInspectionTasks(@Param("planId") Long planId);

    /**
     * 按工单状态分组统计数量。
     *
     * @return 分组结果列表，每项含 group_key（状态数值）与 cnt（数量）
     */
    List<Map<String, Object>> countGroupByStatus();

    /**
     * 按工单类型分组统计数量。
     *
     * @return 分组结果列表，每项含 group_key（类型编码）与 cnt（数量）
     */
    List<Map<String, Object>> countGroupByOrderType();
}
