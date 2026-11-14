package com.tianxin.lifeline.plan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tianxin.lifeline.api.ai.dto.PlanAgentResultDTO;
import com.tianxin.lifeline.api.ai.dto.PlanResourceItemDTO;
import com.tianxin.lifeline.api.ai.dto.PlanTaskItemDTO;
import com.tianxin.lifeline.common.core.exception.BizException;
import com.tianxin.lifeline.common.core.id.SnowflakeIdGenerator;
import com.tianxin.lifeline.common.core.result.ErrorCode;
import com.tianxin.lifeline.common.security.context.SecurityContext;
import com.tianxin.lifeline.plan.adapter.PlanAiAdapter;
import com.tianxin.lifeline.plan.constant.PlanInstanceStatus;
import com.tianxin.lifeline.plan.constant.PlanSource;
import com.tianxin.lifeline.plan.constant.PlanTaskStatus;
import com.tianxin.lifeline.plan.converter.PlanDtoConverter;
import com.tianxin.lifeline.plan.dto.PlanGenerateRequest;
import com.tianxin.lifeline.plan.dto.PlanInstanceVO;
import com.tianxin.lifeline.plan.entity.PlanInstance;
import com.tianxin.lifeline.plan.entity.PlanLog;
import com.tianxin.lifeline.plan.entity.PlanResource;
import com.tianxin.lifeline.plan.entity.PlanTask;
import com.tianxin.lifeline.plan.entity.PlanTemplate;
import com.tianxin.lifeline.plan.mapper.PlanInstanceMapper;
import com.tianxin.lifeline.plan.mapper.PlanLogMapper;
import com.tianxin.lifeline.plan.mapper.PlanResourceMapper;
import com.tianxin.lifeline.plan.mapper.PlanTaskMapper;
import com.tianxin.lifeline.plan.mapper.PlanTemplateMapper;
import com.tianxin.lifeline.plan.service.PlanGenerateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 预案生成业务服务实现。
 *
 * <p>生成链路：定位模板 → 调用 AI 预案适配器 → 结果落库。
 * AI 成功则 source=AI，任务/资源来自 {@link PlanAgentResultDTO}；
 * AI 失败或无效则降级为按模板 content 拆分实例化通用预案（source=TEMPLATE），
 * 接口永不失败。所有主键由 {@link SnowflakeIdGenerator} 手工填充，
 * created_at / updated_at 由 common-data 字段填充器自动填充。
 */
@Slf4j
@Service
public class PlanGenerateServiceImpl implements PlanGenerateService {

    /** 实例编号日期段格式 */
    private static final DateTimeFormatter CODE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 模板内容分点分隔符（中文分号/英文分号） */
    private static final String CONTENT_SPLIT_REGEX = "[;；]";

    /** 模板内容编号前缀正则（如 "1." "2、" "3．"） */
    private static final String CONTENT_NUMBER_PREFIX_REGEX = "^\\d+[.、．]\\s*";

    /** 模板内容引导语前缀 */
    private static final String CONTENT_LEAD_PREFIX = "处置要点：";

    /** 任务名称最大长度（对齐 DDL task_name varchar(128)） */
    private static final int TASK_NAME_MAX_LEN = 128;

    /** 模板 Mapper */
    private final PlanTemplateMapper templateMapper;

    /** 实例 Mapper */
    private final PlanInstanceMapper instanceMapper;

    /** 任务 Mapper */
    private final PlanTaskMapper taskMapper;

    /** 资源 Mapper */
    private final PlanResourceMapper resourceMapper;

    /** 日志 Mapper */
    private final PlanLogMapper logMapper;

    /** AI 预案适配器（失败自动降级模板实例化） */
    private final PlanAiAdapter planAiAdapter;

    /**
     * 构造服务（构造器注入）。
     *
     * @param templateMapper 模板 Mapper
     * @param instanceMapper 实例 Mapper
     * @param taskMapper     任务 Mapper
     * @param resourceMapper 资源 Mapper
     * @param logMapper      日志 Mapper
     * @param planAiAdapter  AI 预案适配器
     */
    public PlanGenerateServiceImpl(PlanTemplateMapper templateMapper,
                                   PlanInstanceMapper instanceMapper,
                                   PlanTaskMapper taskMapper,
                                   PlanResourceMapper resourceMapper,
                                   PlanLogMapper logMapper,
                                   PlanAiAdapter planAiAdapter) {
        this.templateMapper = templateMapper;
        this.instanceMapper = instanceMapper;
        this.taskMapper = taskMapper;
        this.resourceMapper = resourceMapper;
        this.logMapper = logMapper;
        this.planAiAdapter = planAiAdapter;
    }

    /**
     * 生成处置预案实例（AI 优先，失败降级模板实例化，接口永不失败）。
     *
     * @param request 预案生成请求
     * @return 生成成功的预案实例视图
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlanInstanceVO generate(PlanGenerateRequest request) {
        // 1. 定位预案模板（templateId 优先，其次 scenarioType）
        PlanTemplate template = resolveTemplate(request);

        // 2. AI 智能生成（失败自动降级返回 null）
        PlanAgentResultDTO aiResult = planAiAdapter.generate(request);
        boolean fromAi = aiResult != null;
        String source = fromAi ? PlanSource.AI : PlanSource.TEMPLATE;

        // 3. 组装并落库预案实例（雪花 ID 手工填充）
        PlanInstance instance = buildInstance(request, template, aiResult, source);
        instanceMapper.insert(instance);

        // 4. 落库任务清单（AI 任务清单 或 模板 content 拆分的通用任务）
        List<PlanTask> tasks = fromAi
                ? buildTasksFromAi(instance.getId(), aiResult)
                : buildTasksFromTemplate(instance.getId(), template);
        tasks.forEach(taskMapper::insert);

        // 5. 落库资源清单（仅 AI 结果携带，模板降级无资源需求）
        int resourceCount = 0;
        if (fromAi && aiResult.getResources() != null) {
            for (PlanResourceItemDTO item : aiResult.getResources()) {
                resourceMapper.insert(buildResource(instance.getId(), item, request.getLocationDesc()));
                resourceCount++;
            }
        }

        // 6. 写执行日志留痕
        writeGenerateLog(instance, source, tasks.size(), resourceCount);
        log.info("预案生成成功: instanceId={}, instanceCode={}, source={}, tasks={}, resources={}",
                instance.getId(), instance.getInstanceCode(), source, tasks.size(), resourceCount);

        PlanInstanceVO vo = PlanDtoConverter.toInstanceVO(instance);
        vo.setTaskTotal(tasks.size());
        vo.setTaskCompleted(0);
        return vo;
    }

    /* ==================== 私有辅助方法 ==================== */

    /**
     * 定位预案模板：templateId 优先，其次 scenarioType（仅匹配启用模板），
     * 两者皆空或匹配失败抛业务异常。
     *
     * @param request 预案生成请求
     * @return 预案模板
     */
    private PlanTemplate resolveTemplate(PlanGenerateRequest request) {
        if (request.getTemplateId() != null) {
            PlanTemplate template = templateMapper.selectById(request.getTemplateId());
            if (template == null) {
                throw new BizException(ErrorCode.PLAN_NOT_FOUND, "指定的预案模板不存在");
            }
            return template;
        }
        if (StringUtils.hasText(request.getScenarioType())) {
            LambdaQueryWrapper<PlanTemplate> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PlanTemplate::getScenarioType, request.getScenarioType())
                    .eq(PlanTemplate::getStatus, 1)
                    .orderByAsc(PlanTemplate::getId)
                    .last("LIMIT 1");
            PlanTemplate template = templateMapper.selectOne(wrapper);
            if (template == null) {
                throw new BizException(ErrorCode.PLAN_NOT_FOUND,
                        "场景类型对应预案模板不存在或未启用：" + request.getScenarioType());
            }
            return template;
        }
        throw new BizException(ErrorCode.PARAM_INVALID, "templateId 与 scenarioType 至少提供其一");
    }

    /**
     * 组装预案实例（状态取草稿，编号 PLAN-yyyyMMdd-雪花ID）。
     *
     * @param request  预案生成请求
     * @param template 预案模板
     * @param aiResult AI 生成结果（降级时为 null）
     * @param source   实例来源
     * @return 预案实例实体（已填充雪花ID）
     */
    private PlanInstance buildInstance(PlanGenerateRequest request, PlanTemplate template,
                                       PlanAgentResultDTO aiResult, String source) {
        long instanceId = SnowflakeIdGenerator.getInstance().nextId();
        PlanInstance instance = new PlanInstance();
        instance.setId(instanceId);
        instance.setInstanceCode("PLAN-" + CODE_DATE_FORMAT.format(LocalDateTime.now()) + "-" + instanceId);
        instance.setTemplateId(template.getId());
        instance.setWarningEventId(request.getWarningEventId());
        instance.setStatus(PlanInstanceStatus.DRAFT);
        instance.setGeneratedBy(SecurityContext.getUserId());
        instance.setSource(source);
        // AI 结果的预案名称优先，降级时取模板名称
        instance.setPlanName(aiResult != null && StringUtils.hasText(aiResult.getPlanName())
                ? aiResult.getPlanName() : template.getTemplateName());
        // 综合处置建议：AI suggestion 优先，降级时取模板内容
        instance.setSuggestion(aiResult != null && StringUtils.hasText(aiResult.getSuggestion())
                ? aiResult.getSuggestion() : template.getContent());
        return instance;
    }

    /**
     * 由 AI 结果构建任务清单（suggestAssignee → assignee）。
     *
     * @param instanceId 实例ID
     * @param aiResult   AI 生成结果
     * @return 任务实体列表（已填充雪花ID）
     */
    private List<PlanTask> buildTasksFromAi(long instanceId, PlanAgentResultDTO aiResult) {
        List<PlanTask> tasks = new ArrayList<>();
        for (PlanTaskItemDTO item : aiResult.getTasks()) {
            PlanTask task = new PlanTask();
            task.setId(SnowflakeIdGenerator.getInstance().nextId());
            task.setInstanceId(instanceId);
            task.setTaskName(truncate(item.getTaskName(), TASK_NAME_MAX_LEN));
            task.setTaskType(StringUtils.hasText(item.getTaskType()) ? item.getTaskType() : "GENERAL");
            task.setAssignee(item.getSuggestAssignee());
            task.setStatus(PlanTaskStatus.PENDING);
            task.setPriority(normalizePriority(item.getPriority()));
            tasks.add(task);
        }
        return tasks;
    }

    /**
     * 降级链路：按模板 content 拆分编号要点实例化通用任务，
     * 无法拆分时兜底单条总任务，保证降级后仍有可执行任务。
     *
     * @param instanceId 实例ID
     * @param template   预案模板
     * @return 任务实体列表（已填充雪花ID）
     */
    private List<PlanTask> buildTasksFromTemplate(long instanceId, PlanTemplate template) {
        List<PlanTask> tasks = new ArrayList<>();
        String content = template.getContent() == null ? "" : template.getContent();
        String body = content.startsWith(CONTENT_LEAD_PREFIX)
                ? content.substring(CONTENT_LEAD_PREFIX.length()) : content;
        for (String part : body.split(CONTENT_SPLIT_REGEX)) {
            String taskName = part.trim().replaceFirst(CONTENT_NUMBER_PREFIX_REGEX, "").trim();
            if (!StringUtils.hasText(taskName)) {
                continue;
            }
            PlanTask task = new PlanTask();
            task.setId(SnowflakeIdGenerator.getInstance().nextId());
            task.setInstanceId(instanceId);
            task.setTaskName(truncate(taskName, TASK_NAME_MAX_LEN));
            task.setTaskType("GENERAL");
            task.setStatus(PlanTaskStatus.PENDING);
            task.setPriority(2);
            tasks.add(task);
        }
        if (tasks.isEmpty()) {
            PlanTask task = new PlanTask();
            task.setId(SnowflakeIdGenerator.getInstance().nextId());
            task.setInstanceId(instanceId);
            task.setTaskName("按预案模板【" + template.getTemplateName() + "】执行应急处置");
            task.setTaskType("GENERAL");
            task.setStatus(PlanTaskStatus.PENDING);
            task.setPriority(2);
            tasks.add(task);
        }
        return tasks;
    }

    /**
     * 由 AI 结果构建资源条目（location 取请求中的位置描述）。
     *
     * @param instanceId 实例ID
     * @param item       AI 资源条目
     * @param location   位置描述
     * @return 资源实体（已填充雪花ID）
     */
    private PlanResource buildResource(long instanceId, PlanResourceItemDTO item, String location) {
        PlanResource resource = new PlanResource();
        resource.setId(SnowflakeIdGenerator.getInstance().nextId());
        resource.setInstanceId(instanceId);
        resource.setResourceType(StringUtils.hasText(item.getResourceType()) ? item.getResourceType() : "MATERIAL");
        resource.setResourceName(item.getResourceName());
        resource.setQuantity(item.getQuantity() == null ? null : BigDecimal.valueOf(item.getQuantity()));
        resource.setLocation(location);
        resource.setStatus(0);
        return resource;
    }

    /**
     * 写预案生成日志。
     *
     * @param instance      预案实例
     * @param source        实例来源
     * @param taskCount     任务数量
     * @param resourceCount 资源数量
     */
    private void writeGenerateLog(PlanInstance instance, String source, int taskCount, int resourceCount) {
        PlanLog planLog = new PlanLog();
        planLog.setId(SnowflakeIdGenerator.getInstance().nextId());
        planLog.setInstanceId(instance.getId());
        planLog.setAction("GENERATE");
        planLog.setOperator(currentOperator());
        planLog.setContent(PlanSource.AI.equals(source)
                ? "AI智能生成预案，任务" + taskCount + "项、资源" + resourceCount + "项"
                : "AI降级，按模板内容实例化通用预案，任务" + taskCount + "项");
        planLog.setLogTime(LocalDateTime.now());
        logMapper.insert(planLog);
    }

    /**
     * 优先级归一化：null 或越界取默认值 2，合法范围 1-3。
     *
     * @param priority 入参优先级
     * @return 合法优先级
     */
    private Integer normalizePriority(Integer priority) {
        if (priority == null || priority < 1 || priority > 3) {
            return 2;
        }
        return priority;
    }

    /**
     * 字符串截断（超过最大长度截断，避免超出 DDL 列宽）。
     *
     * @param text      原始文本
     * @param maxLength 最大长度
     * @return 截断后的文本，空白返回"未命名任务"
     */
    private String truncate(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return "未命名任务";
        }
        String trimmed = text.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    /**
     * 当前操作人（登录用户名，未登录取 system）。
     *
     * @return 操作人标识
     */
    private String currentOperator() {
        String username = SecurityContext.getUsername();
        return StringUtils.hasText(username) ? username : "system";
    }
}
