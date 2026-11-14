package com.tianxin.lifeline.construction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tianxin.lifeline.api.ai.dto.VisionAnalyzeRequestDTO;
import com.tianxin.lifeline.api.ai.dto.VisionAnalyzeResultDTO;
import com.tianxin.lifeline.common.core.exception.BizException;
import com.tianxin.lifeline.common.core.id.SnowflakeIdGenerator;
import com.tianxin.lifeline.common.core.result.ErrorCode;
import com.tianxin.lifeline.construction.adapter.VisionAdapter;
import com.tianxin.lifeline.construction.constant.ConstructionRedisKeys;
import com.tianxin.lifeline.construction.constant.ProcessQcStatus;
import com.tianxin.lifeline.construction.dto.ProcessCreateRequest;
import com.tianxin.lifeline.construction.dto.ProcessInspectRequest;
import com.tianxin.lifeline.construction.dto.VisionInspectRequest;
import com.tianxin.lifeline.construction.dto.VisionInspectResultVO;
import com.tianxin.lifeline.construction.entity.ConstructionProcess;
import com.tianxin.lifeline.construction.entity.ConstructionProject;
import com.tianxin.lifeline.construction.entity.ConstructionViolation;
import com.tianxin.lifeline.construction.mapper.ConstructionProcessMapper;
import com.tianxin.lifeline.construction.mapper.ConstructionProjectMapper;
import com.tianxin.lifeline.construction.service.ConstructionProcessService;
import com.tianxin.lifeline.construction.service.ConstructionViolationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 施工工序业务服务实现。
 *
 * <p>人工质检回写质检人/时间/得分，得分达合格线记通过、否则不通过；
 * AI 视觉质检调 {@link VisionAdapter}（永不失败），命中违规落违规记录
 * 并经存证生产者投递区块链存证；AI 降级时工序保持质检中，
 * 质检人记为 AI-VISION:摄像头ID，转人工复核。
 */
@Slf4j
@Service
public class ConstructionProcessServiceImpl implements ConstructionProcessService {

    /** 工序 Mapper */
    private final ConstructionProcessMapper processMapper;

    /** 项目 Mapper */
    private final ConstructionProjectMapper projectMapper;

    /** 视觉质检适配器（永不失败） */
    private final VisionAdapter visionAdapter;

    /** 违规业务服务 */
    private final ConstructionViolationService violationService;

    /** Redis 模板（同工序视觉质检并发锁） */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 构造服务（构造器注入）。
     *
     * @param processMapper       工序 Mapper
     * @param projectMapper       项目 Mapper
     * @param visionAdapter       视觉质检适配器
     * @param violationService    违规业务服务
     * @param stringRedisTemplate Redis 模板
     */
    public ConstructionProcessServiceImpl(ConstructionProcessMapper processMapper,
                                          ConstructionProjectMapper projectMapper,
                                          VisionAdapter visionAdapter,
                                          ConstructionViolationService violationService,
                                          StringRedisTemplate stringRedisTemplate) {
        this.processMapper = processMapper;
        this.projectMapper = projectMapper;
        this.visionAdapter = visionAdapter;
        this.violationService = violationService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 查询项目下的工序清单（创建时间升序）。
     *
     * @param projectId 项目ID
     * @return 工序列表
     */
    @Override
    public List<ConstructionProcess> listByProject(Long projectId) {
        LambdaQueryWrapper<ConstructionProcess> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConstructionProcess::getProjectId, projectId)
                .orderByAsc(ConstructionProcess::getCreatedAt).orderByAsc(ConstructionProcess::getId);
        return processMapper.selectList(wrapper);
    }

    /**
     * 创建工序（雪花 ID 手工填充，初始质检状态待质检）。
     *
     * @param projectId 项目ID
     * @param request   工序创建请求
     * @return 工序实体
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConstructionProcess create(Long projectId, ProcessCreateRequest request) {
        loadProject(projectId);
        ConstructionProcess process = new ConstructionProcess();
        process.setId(SnowflakeIdGenerator.getInstance().nextId());
        process.setProjectId(projectId);
        process.setProcessName(request.getProcessName().trim());
        process.setProcessType(request.getProcessType().trim());
        process.setStatus(ProcessQcStatus.PENDING);
        processMapper.insert(process);
        log.info("创建施工工序: id={}, projectId={}, name={}", process.getId(), projectId, process.getProcessName());
        return process;
    }

    /**
     * 提交人工质检记录（回写质检人/时间/得分，得分达合格线记通过）。
     *
     * @param processId 工序ID
     * @param request   质检提交请求
     * @return 回写后的工序实体
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConstructionProcess inspect(Long processId, ProcessInspectRequest request) {
        ConstructionProcess process = loadProcess(processId);
        process.setInspector(request.getInspector().trim());
        process.setQualityScore(request.getQualityScore());
        process.setInspectTime(LocalDateTime.now());
        boolean passed = request.getQualityScore()
                .compareTo(BigDecimal.valueOf(ProcessQcStatus.PASS_LINE)) >= 0;
        process.setStatus(passed ? ProcessQcStatus.PASSED : ProcessQcStatus.REJECTED);
        processMapper.updateById(process);
        log.info("工序人工质检记录提交: processId={}, score={}, result={}",
                processId, request.getQualityScore(), ProcessQcStatus.toText(process.getStatus()));
        return process;
    }

    /**
     * AI 视觉质检：适配器永不失败；分析结果回写工序质检记录，
     * 命中违规落违规记录并投递存证；降级时保持质检中转人工复核。
     *
     * @param processId 工序ID
     * @param request   视觉质检请求
     * @return 视觉质检结果视图
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public VisionInspectResultVO visionInspect(Long processId, VisionInspectRequest request) {
        ConstructionProcess process = loadProcess(processId);
        ConstructionProject project = loadProject(process.getProjectId());

        // 同工序视觉质检并发锁：SETNX 短 TTL 防重复发起；锁值写 UUID，
        // 释放时先 GET 比对归属再 DEL，避免误删 TTL 过期后他人重获的锁
        String lockKey = ConstructionRedisKeys.visionInspectLock(processId);
        String lockToken = UUID.randomUUID().toString();
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockToken, Duration.ofSeconds(60));
        if (!Boolean.TRUE.equals(locked)) {
            throw new BizException(ErrorCode.DUPLICATE_REQUEST, "该工序视觉质检进行中，请勿重复提交");
        }
        try {
            VisionAnalyzeRequestDTO analyzeRequest = new VisionAnalyzeRequestDTO();
            analyzeRequest.setCameraId(request.getCameraId());
            analyzeRequest.setProjectId(project.getId());
            analyzeRequest.setMediaUrl(request.getMediaUrl());
            analyzeRequest.setMediaType(StringUtils.hasText(request.getMediaType())
                    ? request.getMediaType() : "image");
            // 适配器契约：永不失败，AI 不可用返回「未识别/人工复核」降级结论
            VisionAnalyzeResultDTO analyzeResult = visionAdapter.analyze(analyzeRequest);
            boolean degraded = VisionAdapter.isDegraded(analyzeResult);
            boolean hitViolation = !degraded && Boolean.TRUE.equals(analyzeResult.getViolation());

            LocalDateTime now = LocalDateTime.now();
            String inspector = "AI-VISION"
                    + (StringUtils.hasText(request.getCameraId()) ? ":" + request.getCameraId() : "");

            VisionInspectResultVO vo = new VisionInspectResultVO();
            vo.setProcessId(processId);
            vo.setDegraded(degraded);
            vo.setViolation(analyzeResult.getViolation());
            vo.setViolationType(analyzeResult.getViolationType());
            vo.setConfidence(analyzeResult.getConfidence());
            vo.setEvidenceUrl(analyzeResult.getEvidenceUrl());
            vo.setDescription(analyzeResult.getDescription());

            process.setInspector(inspector);
            process.setInspectTime(now);
            if (degraded) {
                // 降级：未识别转人工复核，工序保持质检中，不写得分与违规
                process.setStatus(ProcessQcStatus.INSPECTING);
            } else {
                BigDecimal score = toScore(analyzeResult.getConfidence());
                process.setQualityScore(score);
                vo.setQualityScore(score);
                if (hitViolation) {
                    process.setStatus(ProcessQcStatus.REJECTED);
                    ConstructionViolation violation = violationService.createFromVision(
                            project.getId(), project.getProjectName(),
                            analyzeResult.getViolationType(), analyzeResult.getDescription(),
                            analyzeResult.getEvidenceUrl(), inspector);
                    vo.setViolationId(violation.getId());
                } else {
                    process.setStatus(ProcessQcStatus.PASSED);
                }
            }
            processMapper.updateById(process);

            vo.setProcessStatus(process.getStatus());
            vo.setProcessStatusText(ProcessQcStatus.toText(process.getStatus()));
            log.info("工序AI视觉质检完成: processId={}, degraded={}, hitViolation={}, qcStatus={}",
                    processId, degraded, hitViolation, ProcessQcStatus.toText(process.getStatus()));
            return vo;
        } finally {
            releaseVisionLock(lockKey, lockToken, processId);
        }
    }

    /**
     * 释放视觉质检锁：先 GET 比对锁值归属，一致才 DEL；
     * 锁已过期/被他人重获时不删（容忍 GET→DEL 间极小竞态）。
     *
     * @param lockKey   锁键
     * @param lockToken 加锁时写入的 UUID 锁值
     * @param processId 工序ID（仅日志用）
     */
    private void releaseVisionLock(String lockKey, String lockToken, Long processId) {
        try {
            String current = stringRedisTemplate.opsForValue().get(lockKey);
            if (lockToken.equals(current)) {
                stringRedisTemplate.delete(lockKey);
            } else {
                log.warn("视觉质检锁归属已变更或已过期, 跳过释放: processId={}", processId);
            }
        } catch (Exception e) {
            log.warn("视觉质检锁释放异常, 等待 TTL 自然过期: processId={}", processId, e);
        }
    }

    /* ==================== 私有辅助方法 ==================== */

    /**
     * 加载工序，不存在抛业务异常。
     *
     * @param processId 工序ID
     * @return 工序实体
     */
    private ConstructionProcess loadProcess(Long processId) {
        ConstructionProcess process = processMapper.selectById(processId);
        if (process == null) {
            throw new BizException(ErrorCode.CONSTRUCTION_NOT_FOUND, "施工工序不存在");
        }
        return process;
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

    /**
     * 置信度（0-1）折算百分制质检得分，保留两位小数。
     *
     * @param confidence 识别置信度
     * @return 百分制得分，置信度为空返回 0
     */
    private BigDecimal toScore(Double confidence) {
        double value = confidence == null ? 0.0 : Math.max(0.0, Math.min(1.0, confidence));
        return BigDecimal.valueOf(value * 100).setScale(2, RoundingMode.HALF_UP);
    }
}
