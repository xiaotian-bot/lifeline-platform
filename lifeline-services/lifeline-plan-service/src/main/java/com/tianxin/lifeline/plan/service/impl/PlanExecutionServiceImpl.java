package com.tianxin.lifeline.plan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianxin.lifeline.api.operation.OperationFeignClient;
import com.tianxin.lifeline.api.operation.dto.OrderCreateDTO;
import com.tianxin.lifeline.common.core.page.PageResult;
import com.tianxin.lifeline.common.core.exception.BizException;
import com.tianxin.lifeline.common.core.id.SnowflakeIdGenerator;
import com.tianxin.lifeline.common.core.result.ErrorCode;
import com.tianxin.lifeline.common.core.result.Result;
import com.tianxin.lifeline.common.security.context.SecurityContext;
import com.tianxin.lifeline.plan.constant.PlanEscalationLevel;
import com.tianxin.lifeline.plan.constant.PlanInstanceStatus;
import com.tianxin.lifeline.plan.constant.PlanTaskStatus;
import com.tianxin.lifeline.plan.converter.PlanDtoConverter;
import com.tianxin.lifeline.plan.dto.DisposalFeedbackPayload;
import com.tianxin.lifeline.plan.dto.PlanDetailVO;
import com.tianxin.lifeline.plan.dto.PlanInstanceVO;
import com.tianxin.lifeline.plan.dto.PlanTaskUpdateRequest;
import com.tianxin.lifeline.plan.dto.PlanTaskVO;
import com.tianxin.lifeline.plan.entity.PlanInstance;
import com.tianxin.lifeline.plan.entity.PlanLog;
import com.tianxin.lifeline.plan.entity.PlanResource;
import com.tianxin.lifeline.plan.entity.PlanTask;
import com.tianxin.lifeline.plan.mapper.PlanInstanceMapper;
import com.tianxin.lifeline.plan.mapper.PlanLogMapper;
import com.tianxin.lifeline.plan.mapper.PlanResourceMapper;
import com.tianxin.lifeline.plan.mapper.PlanTaskMapper;
import com.tianxin.lifeline.plan.service.PlanExecutionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 预案执行业务服务实现。
 *
 * <p>实例状态机：0 草稿 →(execute)→ 1 执行中 →(全部任务完成)→ 2 已完成；
 * 1 执行中可取消→ 4，2 已完成可归档→ 3；非法流转抛 {@link BizException}。
 * 状态流转均采用条件更新按影响行数判定，规避并发竞态。
 * 任务状态推进后统计未完成任务，全部终态且实例执行中时自动核销实例。
 * 每步写 plan_log 留痕。execute 可选经 {@link OperationFeignClient#createOrder} 派单，
 * 派单失败仅记日志不阻断预案执行。
 */
@Slf4j
@Service
public class PlanExecutionServiceImpl implements PlanExecutionService {

    /** 实例 Mapper */
    private final PlanInstanceMapper instanceMapper;

    /** 任务 Mapper */
    private final PlanTaskMapper taskMapper;

    /** 资源 Mapper */
    private final PlanResourceMapper resourceMapper;

    /** 日志 Mapper */
    private final PlanLogMapper logMapper;

    /** 工单服务 Feign 客户端（派单联动） */
    private final OperationFeignClient operationFeignClient;

    /**
     * 构造服务（构造器注入）。
     *
     * @param instanceMapper       实例 Mapper
     * @param taskMapper           任务 Mapper
     * @param resourceMapper       资源 Mapper
     * @param logMapper            日志 Mapper
     * @param operationFeignClient 工单服务 Feign 客户端
     */
    public PlanExecutionServiceImpl(PlanInstanceMapper instanceMapper,
                                    PlanTaskMapper taskMapper,
                                    PlanResourceMapper resourceMapper,
                                    PlanLogMapper logMapper,
                                    OperationFeignClient operationFeignClient) {
        this.instanceMapper = instanceMapper;
        this.taskMapper = taskMapper;
        this.resourceMapper = resourceMapper;
        this.logMapper = logMapper;
        this.operationFeignClient = operationFeignClient;
    }

    /**
     * 查询预案实例详情（多表关联实例 + 任务进度聚合 + 任务/资源/日志）。
     *
     * @param instanceId 实例ID
     * @return 详情聚合视图
     */
    @Override
    public PlanDetailVO detail(Long instanceId) {
        PlanInstanceVO instanceVO = instanceMapper.selectInstanceDetail(instanceId);
        if (instanceVO == null) {
            throw new BizException(ErrorCode.PLAN_NOT_FOUND);
        }
        PlanDtoConverter.fillStatusText(instanceVO);
        fillTaskProgress(instanceVO);

        PlanDetailVO detail = new PlanDetailVO();
        detail.setInstance(instanceVO);
        detail.setTasks(listTasks(instanceId));

        LambdaQueryWrapper<PlanResource> resourceWrapper = new LambdaQueryWrapper<>();
        resourceWrapper.eq(PlanResource::getInstanceId, instanceId)
                .orderByAsc(PlanResource::getId);
        detail.setResources(resourceMapper.selectList(resourceWrapper).stream()
                .map(PlanDtoConverter::toResourceVO).toList());

        LambdaQueryWrapper<PlanLog> logWrapper = new LambdaQueryWrapper<>();
        logWrapper.eq(PlanLog::getInstanceId, instanceId)
                .orderByDesc(PlanLog::getLogTime).orderByDesc(PlanLog::getId);
        detail.setLogs(logMapper.selectList(logWrapper).stream()
                .map(PlanDtoConverter::toLogVO).toList());
        return detail;
    }

    /**
     * 启动执行预案（状态机：仅草稿可启动），可选联动派单。
     *
     * @param instanceId    实例ID
     * @param dispatchOrder 是否联动工单服务派单
     * @return 流转后的实例视图
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlanInstanceVO execute(Long instanceId, boolean dispatchOrder) {
        loadInstance(instanceId);
        LocalDateTime now = LocalDateTime.now();
        // 条件更新防并发竞态：仅草稿状态可 CAS 流转，影响行数为 0 即并发失败或状态非法
        LambdaUpdateWrapper<PlanInstance> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(PlanInstance::getId, instanceId)
                .eq(PlanInstance::getStatus, PlanInstanceStatus.DRAFT)
                .set(PlanInstance::getStatus, PlanInstanceStatus.RUNNING)
                .set(PlanInstance::getStartTime, now);
        int affected = instanceMapper.update(null, updateWrapper);
        if (affected == 0) {
            throw new BizException(ErrorCode.PLAN_STATUS_INVALID, "仅草稿状态的预案可启动执行");
        }

        PlanInstance instance = loadInstance(instanceId);
        writeLog(instanceId, "START", "预案启动执行");
        log.info("预案启动执行: instanceId={}, instanceCode={}", instanceId, instance.getInstanceCode());

        if (dispatchOrder) {
            dispatchEmergencyOrder(instance);
        }
        return PlanDtoConverter.toInstanceVO(instance);
    }

    /**
     * 查询实例下的任务清单（优先级升序、创建时间升序）。
     *
     * @param instanceId 实例ID
     * @return 任务视图列表
     */
    @Override
    public List<PlanTaskVO> listTasks(Long instanceId) {
        LambdaQueryWrapper<PlanTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlanTask::getInstanceId, instanceId)
                .orderByAsc(PlanTask::getPriority).orderByAsc(PlanTask::getCreatedAt).orderByAsc(PlanTask::getId);
        return taskMapper.selectList(wrapper).stream().map(PlanDtoConverter::toTaskVO).toList();
    }

    /**
     * 推进任务状态：终态任务不允许再流转；进入执行中写 start_time，
     * 完成/取消写 end_time；全部任务终态后实例自动核销。
     *
     * @param taskId  任务ID
     * @param request 状态推进请求
     * @return 流转后的任务视图
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlanTaskVO updateTask(Long taskId, PlanTaskUpdateRequest request) {
        PlanTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BizException(ErrorCode.PLAN_NOT_FOUND, "预案任务不存在");
        }
        if (PlanTaskStatus.isTerminal(task.getStatus())) {
            throw new BizException(ErrorCode.PLAN_STATUS_INVALID,
                    "任务已处于终态，不允许再流转，当前状态：" + PlanTaskStatus.toText(task.getStatus()));
        }
        Integer target = request.getStatus();
        if (target == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "目标状态不能为空");
        }
        Integer current = task.getStatus();
        if (current == null) {
            throw new BizException(ErrorCode.PLAN_STATUS_INVALID, "任务当前状态缺失，无法流转");
        }
        if (target.equals(current)) {
            throw new BizException(ErrorCode.PLAN_STATUS_INVALID,
                    "任务已处于" + PlanTaskStatus.toText(target) + "状态，请勿重复操作");
        }
        // 流转矩阵校验：待执行仅可→执行中/已取消；执行中仅可→已完成/已取消；禁止跳级
        boolean allowed = (current == PlanTaskStatus.PENDING
                        && (target == PlanTaskStatus.PROCESSING || target == PlanTaskStatus.CANCELLED))
                || (current == PlanTaskStatus.PROCESSING
                        && (target == PlanTaskStatus.COMPLETED || target == PlanTaskStatus.CANCELLED));
        if (!allowed) {
            throw new BizException(ErrorCode.PLAN_STATUS_INVALID,
                    "任务状态不允许从" + PlanTaskStatus.toText(current)
                            + "流转至" + PlanTaskStatus.toText(target));
        }

        String fromText = PlanTaskStatus.toText(current);
        task.setStatus(target);
        LocalDateTime now = LocalDateTime.now();
        if (target == PlanTaskStatus.PROCESSING) {
            task.setStartTime(now);
        } else {
            task.setEndTime(now);
        }
        if (StringUtils.hasText(request.getAssignee())) {
            task.setAssignee(request.getAssignee());
        }
        taskMapper.updateById(task);

        String logContent = "任务【" + task.getTaskName() + "】状态：" + fromText
                + " → " + PlanTaskStatus.toText(target)
                + (StringUtils.hasText(request.getRemark()) ? "，备注：" + request.getRemark() : "");
        writeLog(task.getInstanceId(), "TASK_UPDATE", logContent);

        // 全部任务进入终态且实例执行中 → 实例核销
        tryCompleteInstance(task.getInstanceId());
        return PlanDtoConverter.toTaskVO(task);
    }

    /**
     * 处置反馈回写：按 warningEventId 定位执行中实例，
     * 未完成任务批量置为已完成并核销实例，全程写日志留痕。
     *
     * @param payload 处置反馈负载
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyDisposalFeedback(DisposalFeedbackPayload payload) {
        if (payload == null || payload.getWarningEventId() == null) {
            return;
        }
        List<PlanInstance> instances = instanceMapper.selectByWarningEventId(
                payload.getWarningEventId(), PlanInstanceStatus.RUNNING);
        if (instances.isEmpty()) {
            log.debug("处置反馈无执行中关联预案实例, 已跳过: warningEventId={}", payload.getWarningEventId());
            return;
        }
        String orderInfo = StringUtils.hasText(payload.getOrderCode())
                ? payload.getOrderCode() : String.valueOf(payload.getOrderId());
        LocalDateTime now = LocalDateTime.now();
        for (PlanInstance instance : instances) {
            // 未完成任务批量回写为已完成
            LambdaUpdateWrapper<PlanTask> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(PlanTask::getInstanceId, instance.getId())
                    .in(PlanTask::getStatus, PlanTaskStatus.PENDING, PlanTaskStatus.PROCESSING)
                    .set(PlanTask::getStatus, PlanTaskStatus.COMPLETED)
                    .set(PlanTask::getEndTime, now)
                    .set(PlanTask::getUpdatedAt, now);
            int updated = taskMapper.update(null, updateWrapper);

            // 实例核销：条件更新防并发重复核销（仅执行中可流转）
            if (!completeRunningInstance(instance.getId(), now)) {
                log.warn("处置反馈核销跳过(实例状态已非执行中): instanceId={}", instance.getId());
            }

            writeLog(instance.getId(), "DISPOSAL_WRITEBACK",
                    "收到工单[" + orderInfo + "]处置反馈，回写完成任务" + updated + "项，预案实例核销"
                            + (StringUtils.hasText(payload.getResult()) ? "，处置结果：" + payload.getResult() : ""));
            log.info("处置反馈回写完成: instanceId={}, warningEventId={}, 回写任务数={}",
                    instance.getId(), payload.getWarningEventId(), updated);
        }
    }

    /**
     * 三级联动上报：仅执行中实例可上报，级别逐级 +1 至上限 2，
     * 写 plan_log（action=ESCALATE）留痕。政府侧外部系统对接
     * 仅留日志/事件位，不做真实集成。
     *
     * @param instanceId 实例ID
     * @return 流转后的实例视图
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlanInstanceVO escalate(Long instanceId) {
        PlanInstance instance = loadInstance(instanceId);
        if (instance.getStatus() == null || instance.getStatus() != PlanInstanceStatus.RUNNING) {
            throw new BizException(ErrorCode.PLAN_STATUS_INVALID,
                    "仅执行中的预案可上报联动级别，当前状态：" + PlanInstanceStatus.toText(instance.getStatus()));
        }
        int currentLevel = instance.getEscalationLevel() == null
                ? PlanEscalationLevel.ENTERPRISE : instance.getEscalationLevel();
        if (currentLevel >= PlanEscalationLevel.MAX_LEVEL) {
            throw new BizException(ErrorCode.PLAN_STATUS_INVALID,
                    "联动级别已达上限（应急联动），不可继续上报");
        }
        int targetLevel = currentLevel + 1;
        // 条件更新防并发竞态：仅执行中且当前级别未变时可逐级 +1
        LambdaUpdateWrapper<PlanInstance> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PlanInstance::getId, instanceId)
                .eq(PlanInstance::getStatus, PlanInstanceStatus.RUNNING)
                .eq(PlanInstance::getEscalationLevel, currentLevel)
                .set(PlanInstance::getEscalationLevel, targetLevel);
        if (instanceMapper.update(null, wrapper) == 0) {
            throw new BizException(ErrorCode.PLAN_STATUS_INVALID, "上报失败，实例状态或级别已变化，请刷新后重试");
        }

        writeLog(instanceId, "ESCALATE",
                "三级联动上报：" + PlanEscalationLevel.toText(currentLevel)
                        + " → " + PlanEscalationLevel.toText(targetLevel)
                        + "（政府侧外部系统对接预留位，本次仅留痕未真实推送）");
        log.info("预案三级联动上报: instanceId={}, level={} -> {}", instanceId, currentLevel, targetLevel);

        instance.setEscalationLevel(targetLevel);
        return PlanDtoConverter.toInstanceVO(instance);
    }

    /**
     * 取消预案：草稿/执行中可取消 → 4 已取消（条件更新按影响行数判定）。
     *
     * @param instanceId 实例ID
     * @return 流转后的实例视图
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlanInstanceVO cancel(Long instanceId) {
        loadInstance(instanceId);
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<PlanInstance> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PlanInstance::getId, instanceId)
                .in(PlanInstance::getStatus, PlanInstanceStatus.DRAFT, PlanInstanceStatus.RUNNING)
                .set(PlanInstance::getStatus, PlanInstanceStatus.CANCELLED)
                .set(PlanInstance::getEndTime, now);
        if (instanceMapper.update(null, wrapper) == 0) {
            throw new BizException(ErrorCode.PLAN_STATUS_INVALID,
                    "仅草稿/执行中的预案可取消，当前状态不允许取消");
        }
        writeLog(instanceId, "CANCEL", "预案取消");
        log.info("预案取消: instanceId={}", instanceId);
        return PlanDtoConverter.toInstanceVO(loadInstance(instanceId));
    }

    /**
     * 归档预案：已完成可归档 → 3 已归档（条件更新按影响行数判定）。
     *
     * @param instanceId 实例ID
     * @return 流转后的实例视图
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlanInstanceVO archive(Long instanceId) {
        loadInstance(instanceId);
        LambdaUpdateWrapper<PlanInstance> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PlanInstance::getId, instanceId)
                .eq(PlanInstance::getStatus, PlanInstanceStatus.COMPLETED)
                .set(PlanInstance::getStatus, PlanInstanceStatus.ARCHIVED);
        if (instanceMapper.update(null, wrapper) == 0) {
            throw new BizException(ErrorCode.PLAN_STATUS_INVALID, "仅已完成的预案可归档");
        }
        writeLog(instanceId, "ARCHIVE", "预案归档");
        log.info("预案归档: instanceId={}", instanceId);
        return PlanDtoConverter.toInstanceVO(loadInstance(instanceId));
    }

    /* ==================== 私有辅助方法 ==================== */

    /**
     * 加载实例，不存在抛业务异常。
     *
     * @param instanceId 实例ID
     * @return 实例实体
     */
    private PlanInstance loadInstance(Long instanceId) {
        PlanInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null) {
            throw new BizException(ErrorCode.PLAN_NOT_FOUND);
        }
        return instance;
    }

    /**
     * 联动工单服务派发应急工单（失败降级：仅记日志不阻断预案执行）。
     *
     * @param instance 已启动的预案实例
     */
    private void dispatchEmergencyOrder(PlanInstance instance) {
        try {
            OrderCreateDTO dto = new OrderCreateDTO();
            dto.setTitle("预案执行应急工单-" + instance.getInstanceCode());
            dto.setOrderType("EMERGENCY");
            dto.setPriority(1);
            dto.setWarningId(instance.getWarningEventId());
            dto.setDescription("预案实例【" + instance.getInstanceCode() + "】启动执行，联动派发应急处置工单。");
            dto.setSource("PLAN");
            Result<Long> result = operationFeignClient.createOrder(dto);
            if (result != null && result.isSuccess()) {
                writeLog(instance.getId(), "DISPATCH_ORDER", "联动派单成功，工单ID：" + result.getData());
                log.info("预案联动派单成功: instanceId={}, orderId={}", instance.getId(), result.getData());
            } else {
                log.warn("预案联动派单返回失败, 已降级跳过: instanceId={}, result={}",
                        instance.getId(), result == null ? "null" : result.getMessage());
            }
        } catch (Exception e) {
            // 派单失败仅记日志不阻断预案执行
            log.warn("预案联动派单调用失败, 已降级跳过: instanceId={}, cause={}",
                    instance.getId(), e.getMessage());
        }
    }

    /**
     * 统计实例任务进度并回填视图对象（任务总数/已完成数）。
     *
     * @param instanceVO 实例视图对象
     */
    private void fillTaskProgress(PlanInstanceVO instanceVO) {
        long total = 0L;
        long completed = 0L;
        for (Map<String, Object> row : instanceMapper.countTaskGroupByStatus(instanceVO.getId())) {
            long cnt = toLong(row.get("cnt"));
            total += cnt;
            Integer statusValue = toInt(row.get("group_key"));
            if (statusValue != null && statusValue == PlanTaskStatus.COMPLETED) {
                completed += cnt;
            }
        }
        instanceVO.setTaskTotal((int) total);
        instanceVO.setTaskCompleted((int) completed);
    }

    /**
     * 实例下全部任务进入终态且实例执行中时，核销实例（2 已完成）。
     *
     * @param instanceId 实例ID
     */
    private void tryCompleteInstance(Long instanceId) {
        LambdaQueryWrapper<PlanTask> unfinishedWrapper = new LambdaQueryWrapper<>();
        unfinishedWrapper.eq(PlanTask::getInstanceId, instanceId)
                .in(PlanTask::getStatus, PlanTaskStatus.PENDING, PlanTaskStatus.PROCESSING);
        if (taskMapper.selectCount(unfinishedWrapper) > 0) {
            return;
        }
        // 条件更新按影响行数判定，防止并发重复核销与重复 COMPLETE 日志
        if (completeRunningInstance(instanceId, LocalDateTime.now())) {
            writeLog(instanceId, "COMPLETE", "全部任务完成，预案实例核销");
            log.info("预案实例核销: instanceId={}", instanceId);
        }
    }

    /**
     * 将执行中实例条件更新为已完成（WHERE status=1 防并发竞态）。
     *
     * @param instanceId 实例ID
     * @param endTime    结束时间
     * @return true 表示本次成功核销；false 表示实例非执行中（已被其他路径核销）
     */
    private boolean completeRunningInstance(Long instanceId, LocalDateTime endTime) {
        LambdaUpdateWrapper<PlanInstance> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PlanInstance::getId, instanceId)
                .eq(PlanInstance::getStatus, PlanInstanceStatus.RUNNING)
                .set(PlanInstance::getStatus, PlanInstanceStatus.COMPLETED)
                .set(PlanInstance::getEndTime, endTime);
        return instanceMapper.update(null, wrapper) > 0;
    }

    /**
     * 写预案执行日志（雪花 ID 手工填充，logTime 取当前时间）。
     *
     * @param instanceId 实例ID
     * @param action     动作编码
     * @param content    日志内容
     */
    private void writeLog(Long instanceId, String action, String content) {
        PlanLog planLog = new PlanLog();
        planLog.setId(SnowflakeIdGenerator.getInstance().nextId());
        planLog.setInstanceId(instanceId);
        planLog.setAction(action);
        String username = SecurityContext.getUsername();
        planLog.setOperator(StringUtils.hasText(username) ? username : "system");
        planLog.setContent(content);
        planLog.setLogTime(LocalDateTime.now());
        logMapper.insert(planLog);
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

    @Override
    public PageResult<PlanInstanceVO> listInstances(int page, int size) {
        LambdaQueryWrapper<PlanInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(PlanInstance::getCreatedAt);
        Page<PlanInstance> pageObj = instanceMapper.selectPage(new Page<>(page, size), wrapper);
        List<PlanInstanceVO> records = pageObj.getRecords().stream()
                .map(inst -> {
                    PlanInstanceVO vo = PlanDtoConverter.toInstanceVO(inst);
                    PlanDtoConverter.fillStatusText(vo);
                    return vo;
                })
                .toList();
        return PageResult.of(records, pageObj.getTotal(), page, size);
    }
}
