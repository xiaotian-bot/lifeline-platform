package com.tianxin.lifeline.operation.service.impl;

import com.tianxin.lifeline.api.operation.dto.OrderCreateDTO;
import com.tianxin.lifeline.common.core.constant.TopicConstants;
import com.tianxin.lifeline.common.core.exception.BizException;
import com.tianxin.lifeline.common.core.id.SnowflakeIdGenerator;
import com.tianxin.lifeline.common.core.page.PageResult;
import com.tianxin.lifeline.common.core.result.ErrorCode;
import com.tianxin.lifeline.common.mq.template.KafkaEventTemplate;
import com.tianxin.lifeline.operation.constant.OrderEventType;
import com.tianxin.lifeline.operation.constant.OrderSource;
import com.tianxin.lifeline.operation.constant.OrderStatus;
import com.tianxin.lifeline.operation.constant.OrderType;
import com.tianxin.lifeline.operation.dto.DisposalFeedbackPayload;
import com.tianxin.lifeline.operation.dto.OrderEventPayload;
import com.tianxin.lifeline.operation.dto.OrderQueryRequest;
import com.tianxin.lifeline.operation.dto.OrderStatisticsVO;
import com.tianxin.lifeline.operation.dto.OrderVO;
import com.tianxin.lifeline.operation.entity.OperationOrder;
import com.tianxin.lifeline.operation.mapper.OperationOrderMapper;
import com.tianxin.lifeline.operation.service.OperationOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 运维工单业务服务实现。
 *
 * <p>状态机：创建（0 待派单 / 1 待接单）→ accept（1→2）→ complete（2→3），
 * 0/1/2 可 close（→4）；非法流转抛 {@link BizException}。
 * 每次状态变更发布 pipe_order_event 事件，complete 额外发布
 * pipe_disposal_event 处置反馈事件（分区键 warningEventId）供预警核销联动。
 */
@Slf4j
@Service
public class OperationOrderServiceImpl implements OperationOrderService {

    /** 工单编号日期段格式 */
    private static final DateTimeFormatter CODE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 排序字段白名单（请求 orderBy → XML 排序键） */
    private static final Set<String> ORDER_BY_WHITELIST = Set.of("priority", "status", "created_at");

    /** 工单 Mapper */
    private final OperationOrderMapper orderMapper;

    /** Kafka 事件发送模板 */
    private final KafkaEventTemplate kafkaEventTemplate;

    /**
     * 构造服务（构造器注入）。
     *
     * @param orderMapper       工单 Mapper
     * @param kafkaEventTemplate Kafka 事件发送模板
     */
    public OperationOrderServiceImpl(OperationOrderMapper orderMapper, KafkaEventTemplate kafkaEventTemplate) {
        this.orderMapper = orderMapper;
        this.kafkaEventTemplate = kafkaEventTemplate;
    }

    /**
     * 创建运维工单（指派责任人取 1 待接单，否则 0 待派单），
     * 成功后发布 ORDER_CREATED 事件。
     *
     * @param dto 工单创建参数
     * @return 创建成功的工单ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(OrderCreateDTO dto) {
        if (dto == null || !OrderType.isValid(dto.getOrderType())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "工单类型不合法，允许值：REPAIR/INSPECT/MAINTAIN/EMERGENCY");
        }
        if (dto.getSource() != null && !OrderSource.isValid(dto.getSource())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "工单来源不合法，允许值：WARNING/PLAN/MANUAL/AI");
        }

        OperationOrder order = new OperationOrder();
        order.setOrderCode(generateOrderCode());
        order.setTitle(StringUtils.hasText(dto.getTitle()) ? dto.getTitle()
                : OrderType.toText(dto.getOrderType()) + "工单");
        order.setOrderType(dto.getOrderType());
        order.setWarningEventId(dto.getWarningId());
        order.setDeviceId(dto.getDeviceId());
        order.setPipeId(dto.getPipeId());
        order.setAreaId(dto.getAreaId());
        order.setSource(StringUtils.hasText(dto.getSource()) ? dto.getSource() : OrderSource.MANUAL);
        order.setContent(dto.getDescription());
        order.setPriority(normalizePriority(dto.getPriority()));
        order.setAssignee(dto.getAssigneeId() == null ? null : String.valueOf(dto.getAssigneeId()));
        order.setStatus(dto.getAssigneeId() == null ? OrderStatus.PENDING_DISPATCH : OrderStatus.PENDING_ACCEPT);
        orderMapper.insert(order);

        publishOrderEvent(order, OrderEventType.ORDER_CREATED);
        log.info("创建运维工单成功: id={}, orderCode={}, status={}", order.getId(), order.getOrderCode(),
                OrderStatus.toText(order.getStatus()));
        return order.getId();
    }

    /**
     * 分页查询工单（红色预警应急工单默认置顶，排序键做白名单校验）。
     *
     * @param request 分页与过滤条件
     * @return 分页结果
     */
    @Override
    public PageResult<OrderVO> page(OrderQueryRequest request) {
        Integer status = resolveStatus(request.getStatus());
        String orderBy = resolveOrderBy(request);

        long total = orderMapper.countOrder(status, request.getOrderType(), request.getWarningEventId(),
                request.getAssignee(), request.getPriority(), request.getStartTime(), request.getEndTime());
        if (total == 0) {
            return PageResult.empty(request.getPage(), request.getSize());
        }
        List<OperationOrder> records = orderMapper.selectOrderPage(status, request.getOrderType(),
                request.getWarningEventId(), request.getAssignee(), request.getPriority(),
                request.getStartTime(), request.getEndTime(), orderBy, request.offset(), request.getSize());
        return PageResult.of(records.stream().map(this::toVO).toList(), total, request.getPage(), request.getSize());
    }

    /**
     * 查询工单详情。
     *
     * @param id 工单ID
     * @return 工单详情
     */
    @Override
    public OrderVO detail(Long id) {
        return toVO(loadOrder(id));
    }

    /**
     * 接单：1 待接单 → 2 处置中，写 start_time，发布 ORDER_ACCEPTED 事件。
     *
     * @param id 工单ID
     * @return 流转后的工单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO accept(Long id) {
        OperationOrder order = loadOrder(id);
        if (order.getStatus() == null || order.getStatus() != OrderStatus.PENDING_ACCEPT) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID,
                    "仅待接单状态的工单可接单，当前状态：" + OrderStatus.toText(order.getStatus()));
        }
        order.setStatus(OrderStatus.PROCESSING);
        order.setStartTime(LocalDateTime.now());
        orderMapper.updateById(order);

        publishOrderEvent(order, OrderEventType.ORDER_ACCEPTED);
        return toVO(order);
    }

    /**
     * 完成：2 处置中 → 3 已完成，写 result / end_time，发布
     * ORDER_COMPLETED 事件与 DISPOSAL_FEEDBACK 处置反馈事件。
     *
     * @param id     工单ID
     * @param result 处置结果描述
     * @return 流转后的工单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO complete(Long id, String result) {
        OperationOrder order = loadOrder(id);
        if (order.getStatus() == null || order.getStatus() != OrderStatus.PROCESSING) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID,
                    "仅处置中状态的工单可完成，当前状态：" + OrderStatus.toText(order.getStatus()));
        }
        order.setStatus(OrderStatus.COMPLETED);
        order.setResult(result);
        order.setEndTime(LocalDateTime.now());
        orderMapper.updateById(order);

        publishOrderEvent(order, OrderEventType.ORDER_COMPLETED);
        publishDisposalFeedback(order);
        return toVO(order);
    }

    /**
     * 关闭：0/1/2 → 4 已关闭，写 end_time，发布 ORDER_CLOSED 事件。
     *
     * @param id 工单ID
     * @return 流转后的工单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO close(Long id) {
        OperationOrder order = loadOrder(id);
        if (OrderStatus.isTerminal(order.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID,
                    "工单已处于终态，不可关闭，当前状态：" + OrderStatus.toText(order.getStatus()));
        }
        order.setStatus(OrderStatus.CLOSED);
        order.setEndTime(LocalDateTime.now());
        orderMapper.updateById(order);

        publishOrderEvent(order, OrderEventType.ORDER_CLOSED);
        return toVO(order);
    }

    /**
     * 按状态与工单类型分组统计。
     *
     * @return 统计结果
     */
    @Override
    public OrderStatisticsVO statistics() {
        OrderStatisticsVO vo = new OrderStatisticsVO();
        vo.setTotal(orderMapper.countOrder(null, null, null, null, null, null, null));

        List<OrderStatisticsVO.GroupCount> byStatus = new ArrayList<>();
        for (Map<String, Object> row : orderMapper.countGroupByStatus()) {
            Integer statusValue = toInt(row.get("groupKey"));
            OrderStatisticsVO.GroupCount item = new OrderStatisticsVO.GroupCount();
            item.setGroupKey(statusValue == null ? "UNKNOWN" : String.valueOf(statusValue));
            item.setGroupText(OrderStatus.toText(statusValue));
            item.setCount(toLong(row.get("cnt")));
            byStatus.add(item);
        }
        vo.setByStatus(byStatus);

        List<OrderStatisticsVO.GroupCount> byType = new ArrayList<>();
        for (Map<String, Object> row : orderMapper.countGroupByOrderType()) {
            Object key = row.get("groupKey");
            String type = key == null ? "UNKNOWN" : String.valueOf(key);
            OrderStatisticsVO.GroupCount item = new OrderStatisticsVO.GroupCount();
            item.setGroupKey(type);
            item.setGroupText(OrderType.toText(type));
            item.setCount(toLong(row.get("cnt")));
            byType.add(item);
        }
        vo.setByOrderType(byType);
        return vo;
    }

    /**
     * 查询预警事件关联工单列表。
     *
     * @param warningEventId 预警事件ID
     * @return 关联工单列表
     */
    @Override
    public List<OrderVO> listByWarningEventId(Long warningEventId) {
        return orderMapper.selectByWarningEventId(warningEventId).stream().map(this::toVO).toList();
    }

    /**
     * 查询巡检计划派生的巡检工单。
     *
     * @param planId 巡检计划ID
     * @return 派生的巡检工单列表
     */
    @Override
    public List<OrderVO> listInspectionTasks(Long planId) {
        return orderMapper.selectInspectionTasks(planId).stream().map(this::toVO).toList();
    }

    /* ==================== 私有辅助方法 ==================== */

    /**
     * 加载工单，不存在抛业务异常。
     *
     * @param id 工单ID
     * @return 工单实体
     */
    private OperationOrder loadOrder(Long id) {
        OperationOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    /**
     * 生成工单编号：GD-yyyyMMdd-雪花ID。
     *
     * @return 工单编号
     */
    private String generateOrderCode() {
        return "GD-" + CODE_DATE_FORMAT.format(LocalDateTime.now()) + "-"
                + SnowflakeIdGenerator.getInstance().nextId();
    }

    /**
     * 优先级归一化：null 或越界时取默认值 2，合法范围 1-4。
     *
     * @param priority 入参优先级
     * @return 合法优先级
     */
    private Integer normalizePriority(Integer priority) {
        if (priority == null || priority < 1 || priority > 4) {
            return 2;
        }
        return priority;
    }

    /**
     * 解析状态过滤条件（兼容中文与数值）。
     *
     * @param statusText 状态文本
     * @return 状态数值，无法解析返回 null
     */
    private Integer resolveStatus(String statusText) {
        if (!StringUtils.hasText(statusText)) {
            return null;
        }
        Integer fromText = OrderStatus.fromText(statusText);
        if (fromText != null) {
            return fromText;
        }
        try {
            int value = Integer.parseInt(statusText.trim());
            return OrderStatus.toText(value).isEmpty() ? null : value;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 解析排序键（白名单校验），不传或非法时返回 null（XML 使用红色预警应急置顶默认排序）。
     *
     * @param request 查询请求
     * @return XML 排序键
     */
    private String resolveOrderBy(OrderQueryRequest request) {
        if (!StringUtils.hasText(request.getOrderBy()) || !ORDER_BY_WHITELIST.contains(request.getOrderBy())) {
            return null;
        }
        if ("created_at".equals(request.getOrderBy())) {
            return request.isAsc() ? "created_at_asc" : null;
        }
        return request.getOrderBy();
    }

    /**
     * 发布工单状态事件至 pipe_order_event（分区键为工单ID）。
     *
     * @param order     工单实体
     * @param eventType 事件类型
     */
    private void publishOrderEvent(OperationOrder order, String eventType) {
        OrderEventPayload payload = new OrderEventPayload();
        payload.setOrderId(order.getId());
        payload.setOrderCode(order.getOrderCode());
        payload.setOrderType(order.getOrderType());
        payload.setStatus(order.getStatus());
        payload.setPriority(order.getPriority());
        payload.setWarningEventId(order.getWarningEventId());
        payload.setAssignee(order.getAssignee());
        payload.setSource(order.getSource());
        kafkaEventTemplate.sendEvent(TopicConstants.PIPE_ORDER_EVENT, String.valueOf(order.getId()),
                eventType, OrderEventType.EVENT_SOURCE, payload);
    }

    /**
     * 发布处置反馈事件至 pipe_disposal_event（分区键 warningEventId，供预警核销联动）。
     *
     * @param order 已完成工单
     */
    private void publishDisposalFeedback(OperationOrder order) {
        DisposalFeedbackPayload payload = new DisposalFeedbackPayload();
        payload.setOrderId(order.getId());
        payload.setOrderCode(order.getOrderCode());
        payload.setWarningEventId(order.getWarningEventId());
        payload.setResult(order.getResult());
        String partitionKey = order.getWarningEventId() == null
                ? String.valueOf(order.getId())
                : String.valueOf(order.getWarningEventId());
        kafkaEventTemplate.sendEvent(TopicConstants.PIPE_DISPOSAL_EVENT, partitionKey,
                OrderEventType.DISPOSAL_FEEDBACK, OrderEventType.EVENT_SOURCE, payload);
    }

    /**
     * 实体转视图对象（附状态与类型中文）。
     *
     * @param order 工单实体
     * @return 视图对象
     */
    private OrderVO toVO(OperationOrder order) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderCode(order.getOrderCode());
        vo.setTitle(order.getTitle());
        vo.setOrderType(order.getOrderType());
        vo.setOrderTypeText(OrderType.toText(order.getOrderType()));
        vo.setStatus(order.getStatus());
        vo.setStatusText(OrderStatus.toText(order.getStatus()));
        vo.setPriority(order.getPriority());
        vo.setPipeId(order.getPipeId());
        vo.setDeviceId(order.getDeviceId());
        vo.setWarningEventId(order.getWarningEventId());
        vo.setAssignee(order.getAssignee());
        vo.setAreaId(order.getAreaId());
        vo.setSource(order.getSource());
        vo.setContent(order.getContent());
        vo.setResult(order.getResult());
        vo.setStartTime(order.getStartTime());
        vo.setEndTime(order.getEndTime());
        vo.setCreatedAt(order.getCreatedAt());
        return vo;
    }

    /**
     * 对象安全转 Integer。
     *
     * @param value 原始值
     * @return Integer，非数值返回 null
     */
    private Integer toInt(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    /**
     * 对象安全转 Long。
     *
     * @param value 原始值
     * @return Long，非数值返回 0
     */
    private Long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }
}
