package com.tianxin.lifeline.construction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.tianxin.lifeline.common.core.exception.BizException;
import com.tianxin.lifeline.common.core.id.SnowflakeIdGenerator;
import com.tianxin.lifeline.common.core.result.ErrorCode;
import com.tianxin.lifeline.common.security.context.SecurityContext;
import com.tianxin.lifeline.construction.config.ConstructionProperties;
import com.tianxin.lifeline.construction.constant.ConstructionRedisKeys;
import com.tianxin.lifeline.construction.constant.ViolationStatus;
import com.tianxin.lifeline.construction.dto.ViolationReportRequest;
import com.tianxin.lifeline.construction.entity.ConstructionProject;
import com.tianxin.lifeline.construction.entity.ConstructionViolation;
import com.tianxin.lifeline.construction.mapper.ConstructionProjectMapper;
import com.tianxin.lifeline.construction.mapper.ConstructionViolationMapper;
import com.tianxin.lifeline.construction.service.ConstructionViolationService;
import com.tianxin.lifeline.construction.service.EvidenceProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;

/**
 * 施工违规业务服务实现。
 *
 * <p>人工上报经 Redis SETNX 幂等拦截窗口内重复提交；幂等键与事务联动，
 * 事务未提交（回滚）时删除幂等键，避免误拦合法重试。违规落库后经
 * {@link EvidenceProducer} 投递 pipe_evidence_request 存证事件，
 * 存证投递延迟至事务提交后触发（afterCommit），避免回滚产生
 * 指向不存在违规的链上存证；存证投递失败不回滚业务落库。
 */
@Slf4j
@Service
public class ConstructionViolationServiceImpl implements ConstructionViolationService {

    /** 违规 Mapper */
    private final ConstructionViolationMapper violationMapper;

    /** 项目 Mapper（取项目名称冗余进存证内容） */
    private final ConstructionProjectMapper projectMapper;

    /** 存证事件生产者 */
    private final EvidenceProducer evidenceProducer;

    /** Redis 模板（上报幂等去重） */
    private final StringRedisTemplate stringRedisTemplate;

    /** 业务参数配置 */
    private final ConstructionProperties constructionProperties;

    /**
     * 构造服务（构造器注入）。
     *
     * @param violationMapper          违规 Mapper
     * @param projectMapper            项目 Mapper
     * @param evidenceProducer         存证事件生产者
     * @param stringRedisTemplate      Redis 模板
     * @param constructionProperties   业务参数配置
     */
    public ConstructionViolationServiceImpl(ConstructionViolationMapper violationMapper,
                                            ConstructionProjectMapper projectMapper,
                                            EvidenceProducer evidenceProducer,
                                            StringRedisTemplate stringRedisTemplate,
                                            ConstructionProperties constructionProperties) {
        this.violationMapper = violationMapper;
        this.projectMapper = projectMapper;
        this.evidenceProducer = evidenceProducer;
        this.stringRedisTemplate = stringRedisTemplate;
        this.constructionProperties = constructionProperties;
    }

    /**
     * 查询项目下的违规记录清单（创建时间降序）。
     *
     * @param projectId 项目ID
     * @return 违规记录列表
     */
    @Override
    public List<ConstructionViolation> listByProject(Long projectId) {
        LambdaQueryWrapper<ConstructionViolation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConstructionViolation::getProjectId, projectId)
                .orderByDesc(ConstructionViolation::getCreatedAt).orderByDesc(ConstructionViolation::getId);
        return violationMapper.selectList(wrapper);
    }

    /**
     * 人工上报违规：Redis 幂等拦截 → 落违规记录 → 事务提交后投递存证事件。
     *
     * @param projectId 项目ID
     * @param request   上报请求
     * @return 违规记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConstructionViolation report(Long projectId, ViolationReportRequest request) {
        ConstructionProject project = loadProject(projectId);

        // 幂等：上报指纹（项目+类型+描述 SHA-256 摘要）SETNX 去重，拦截窗口内重复提交
        String fingerprint = projectId + ":" + request.getViolationType() + ":"
                + sha256Hex(request.getDescription());
        String idemKey = ConstructionRedisKeys.violationReportIdempotent(fingerprint);
        Duration ttl = Duration.ofMinutes(constructionProperties.getIdempotentTtlMinutes());
        Boolean firstSeen = stringRedisTemplate.opsForValue().setIfAbsent(idemKey, "1", ttl);
        if (!Boolean.TRUE.equals(firstSeen)) {
            throw new BizException(ErrorCode.DUPLICATE_REQUEST, "违规上报重复提交，请稍后重试");
        }
        // 幂等键与事务联动：事务回滚时删除幂等键，避免 TTL 内误拦合法重试
        registerIdemKeyCleanup(idemKey);

        String reporter = SecurityContext.getUsername();
        ConstructionViolation violation = insertViolation(projectId, request.getViolationType().trim(),
                request.getDescription(), request.getEvidenceUrl(), ViolationStatus.PENDING);
        sendEvidenceAfterCommit(violation, project.getProjectName(),
                ViolationStatus.SOURCE_MANUAL, StringUtils.hasText(reporter) ? reporter : "manual-report");
        log.info("人工上报施工违规: projectId={}, violationId={}, type={}",
                projectId, violation.getId(), violation.getViolationType());
        return violation;
    }

    /**
     * 由 AI 视觉质检命中结果落违规记录并投递存证事件。
     *
     * @param projectId     项目ID
     * @param projectName   项目名称
     * @param violationType 违规类型
     * @param description   违规描述
     * @param evidenceUrl   证据影像对象键
     * @param reporter      识别来源标识（如 AI-VISION:摄像头ID）
     * @return 违规记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConstructionViolation createFromVision(Long projectId, String projectName, String violationType,
                                                  String description, String evidenceUrl, String reporter) {
        ConstructionViolation violation = insertViolation(projectId, violationType,
                description, evidenceUrl, ViolationStatus.PENDING);
        sendEvidenceAfterCommit(violation, projectName,
                ViolationStatus.SOURCE_VISION, reporter);
        log.info("AI视觉识别施工违规: projectId={}, violationId={}, type={}",
                projectId, violation.getId(), violationType);
        return violation;
    }

    /**
     * 处理违规（0 待处理 → 1 已处理，条件更新按影响行数判定）。
     *
     * @param violationId 违规记录ID
     * @return 处理后的违规记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConstructionViolation handle(Long violationId) {
        ConstructionViolation violation = violationMapper.selectById(violationId);
        if (violation == null) {
            throw new BizException(ErrorCode.CONSTRUCTION_NOT_FOUND, "违规记录不存在");
        }
        LambdaUpdateWrapper<ConstructionViolation> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ConstructionViolation::getId, violationId)
                .eq(ConstructionViolation::getStatus, ViolationStatus.PENDING)
                .set(ConstructionViolation::getStatus, ViolationStatus.HANDLED);
        if (violationMapper.update(null, wrapper) == 0) {
            throw new BizException(ErrorCode.INSPECT_PROCESS_ERROR, "违规记录已处理，请勿重复操作");
        }
        violation.setStatus(ViolationStatus.HANDLED);
        log.info("施工违规处理完成: violationId={}", violationId);
        return violation;
    }

    /* ==================== 私有辅助方法 ==================== */

    /**
     * 存证投递延迟至事务提交后触发，避免回滚产生指向不存在违规的链上存证；
     * 无活动事务时直接投递兜底。
     *
     * @param violation   已落库的违规记录
     * @param projectName 项目名称
     * @param source      违规来源
     * @param reporter    上报人/识别来源标识
     */
    private void sendEvidenceAfterCommit(ConstructionViolation violation, String projectName,
                                         String source, String reporter) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evidenceProducer.sendViolationEvidence(violation, projectName, source, reporter);
                }
            });
            return;
        }
        evidenceProducer.sendViolationEvidence(violation, projectName, source, reporter);
    }

    /**
     * 注册幂等键事务联动清理：事务未提交（回滚）时删除幂等键，
     * 避免回滚后 TTL 内误拦合法重试。
     *
     * @param idemKey 幂等键
     */
    private void registerIdemKeyCleanup(String idemKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_COMMITTED) {
                    return;
                }
                try {
                    stringRedisTemplate.delete(idemKey);
                    log.info("违规上报事务未提交, 幂等键已释放: idemKey={}", idemKey);
                } catch (Exception e) {
                    log.warn("违规上报幂等键释放失败, 等待 TTL 自然过期: idemKey={}", idemKey, e);
                }
            }
        });
    }

    /**
     * 计算字符串 SHA-256 十六进制摘要（替代 String.hashCode，避免碰撞风险）。
     *
     * @param value 原始文本
     * @return 64 位十六进制摘要；算法不可用时退化为 hashCode 十六进制
     */
    private static String sha256Hex(String value) {
        String text = value == null ? "" : value;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(text.hashCode());
        }
    }

    /**
     * 落违规记录（雪花 ID 手工填充，初始待处理）。
     *
     * @param projectId     项目ID
     * @param violationType 违规类型
     * @param description   违规描述
     * @param evidenceUrl   证据影像对象键
     * @param status        初始处理状态
     * @return 违规记录
     */
    private ConstructionViolation insertViolation(Long projectId, String violationType,
                                                  String description, String evidenceUrl, int status) {
        ConstructionViolation violation = new ConstructionViolation();
        violation.setId(SnowflakeIdGenerator.getInstance().nextId());
        violation.setProjectId(projectId);
        violation.setViolationType(violationType);
        violation.setDescription(description);
        violation.setEvidenceUrl(evidenceUrl);
        violation.setStatus(status);
        violationMapper.insert(violation);
        return violation;
    }

    /**
     * 加载项目，不存在抛业务异常。
     *
     * @param projectId 项目ID
     * @return 项目实体
     */
    private ConstructionProject loadProject(Long projectId) {
        ConstructionProject project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BizException(ErrorCode.CONSTRUCTION_NOT_FOUND);
        }
        return project;
    }
}
