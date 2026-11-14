package com.tianxin.lifeline.warning.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.tianxin.lifeline.api.ai.dto.AgentJudgeResultDTO;
import com.tianxin.lifeline.warning.constant.WarningSource;
import com.tianxin.lifeline.warning.entity.WarningEvent;
import com.tianxin.lifeline.warning.mapper.WarningEventMapper;
import com.tianxin.lifeline.warning.service.AiJudgeAdapter;
import com.tianxin.lifeline.warning.service.AiJudgeBackfillService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AI 研判异步回填服务实现。
 *
 * <p>设计要点：
 * <ul>
 *     <li><b>异步化</b>：独立研判线程池（核心 2 / 最大 4 / 队列 1024，
 *     守护线程），提交与执行均不阻塞 Kafka 消费主链路；队列满时直接
 *     丢弃并告警（研判为增强能力，不可反压预警主流程）。</li>
 *     <li><b>幂等去重</b>：以 ai_judged_at 为「已研判」标记——提交前
 *     初筛、回填时条件更新（WHERE ai_judged_at IS NULL）原子去重，
 *     同一预警不重复研判。</li>
 *     <li><b>来源过滤</b>：仅规则（RULE）/AI 来源预警触发研判，
 *     人工录入（MANUAL）跳过。</li>
 *     <li><b>字段边界</b>：仅回填 ai_result 与 ai_judged_at，绝不覆写
 *     ai_score（该列语义为时序异常检测评分，非研判置信度）。</li>
 *     <li><b>降级行为</b>：AI 调用失败/超时/返回无效时适配器返回 null，
 *     维持现状不回填任何字段，仅记录日志。</li>
 *     <li><b>等级复核留痕</b>：研判结果含等级复核意见时仅输出日志，
 *     绝不自动改写预警等级（人工确认优先）。</li>
 * </ul>
 */
@Slf4j
@Service
public class AiJudgeBackfillServiceImpl implements AiJudgeBackfillService {

    /** ai_result 列长度上限（与 10 号脚本 varchar(2000) 对齐） */
    private static final int AI_RESULT_MAX_LENGTH = 2000;

    /** AI 研判适配器（失败自动降级返回 null） */
    private final AiJudgeAdapter aiJudgeAdapter;

    /** 预警事件 Mapper */
    private final WarningEventMapper warningEventMapper;

    /** AI 研判专用线程池（守护线程，不阻止 JVM 退出） */
    private final ExecutorService judgeExecutor;

    /**
     * 构造异步回填服务。
     *
     * @param aiJudgeAdapter     AI 研判适配器
     * @param warningEventMapper 预警事件 Mapper
     */
    public AiJudgeBackfillServiceImpl(AiJudgeAdapter aiJudgeAdapter,
                                      WarningEventMapper warningEventMapper) {
        this.aiJudgeAdapter = aiJudgeAdapter;
        this.warningEventMapper = warningEventMapper;
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger seq = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "ai-judge-backfill-" + seq.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        };
        this.judgeExecutor = new ThreadPoolExecutor(2, 4, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1024), threadFactory,
                (runnable, executor) -> log.warn("AI 研判线程池队列已满, 本次研判任务丢弃（不影响预警主流程）"));
    }

    /**
     * 提交预警事件的异步 AI 研判回填任务（绝不抛异常）。
     *
     * @param saved 已落库的预警事件
     */
    @Override
    public void submitAsyncJudge(WarningEvent saved) {
        if (saved == null || saved.getId() == null) {
            return;
        }
        // 提交前初筛：已有研判结果（ai_judged_at 非空）直接跳过
        if (saved.getAiJudgedAt() != null) {
            log.debug("预警已完成 AI 研判, 跳过重复研判: warningId={}", saved.getId());
            return;
        }
        // 来源过滤：仅规则/AI 来源触发研判，人工录入（MANUAL 及非法来源归一值）跳过
        String source = WarningSource.normalize(saved.getSource());
        if (!WarningSource.RULE.equals(source) && !WarningSource.AI.equals(source)) {
            log.debug("AI 研判跳过(非规则/AI 来源): warningId={}, source={}", saved.getId(), source);
            return;
        }
        Long warningId = saved.getId();
        try {
            judgeExecutor.execute(() -> judgeAndBackfill(warningId));
        } catch (Exception e) {
            // 提交失败（如线程池关闭）仅告警，绝不影响预警主流程
            log.warn("AI 研判任务提交失败, 已忽略: warningId={}, cause={}", warningId, e.getMessage());
        }
    }

    /**
     * 执行研判并回填（线程池内执行，任何异常仅记录日志）。
     *
     * @param warningId 预警事件ID
     */
    private void judgeAndBackfill(Long warningId) {
        try {
            // 二次校验：回查最新记录，已研判/已删除则跳过（幂等去重）
            WarningEvent current = warningEventMapper.selectById(warningId);
            if (current == null) {
                log.debug("AI 研判跳过, 预警事件不存在或已删除: warningId={}", warningId);
                return;
            }
            if (current.getAiJudgedAt() != null) {
                log.debug("AI 研判跳过, 该预警已有研判结果: warningId={}", warningId);
                return;
            }

            AgentJudgeResultDTO result = aiJudgeAdapter.judgeDetail(current);
            if (result == null) {
                // AI 失败/超时/降级：维持现状不回填，等待后续可人工或补偿研判
                log.info("AI 研判降级未回填: warningId={}, eventCode={}", warningId, current.getEventCode());
                return;
            }

            // 等级复核意见仅日志留痕，绝不自动改写预警等级（人工确认优先）
            if (result.getLevelReview() != null && !result.getLevelReview().isBlank()) {
                log.info("AI 等级复核意见(仅留痕, 不改写预警等级): warningId={}, eventCode={}, 当前等级={}, 复核意见={}",
                        warningId, current.getEventCode(), current.getWarningLevel(), result.getLevelReview());
            }

            // 条件更新原子回填：WHERE id=? AND ai_judged_at IS NULL，
            // 消除 check-then-act 竞态窗口；仅写 ai_result 与 ai_judged_at，
            // 不覆写 ai_score（该列语义为时序异常检测评分，非研判置信度）
            LambdaUpdateWrapper<WarningEvent> updateWrapper = new LambdaUpdateWrapper<WarningEvent>()
                    .eq(WarningEvent::getId, warningId)
                    .isNull(WarningEvent::getAiJudgedAt)
                    .set(WarningEvent::getAiResult, buildResultSummary(result))
                    .set(WarningEvent::getAiJudgedAt, LocalDateTime.now());
            int affected = warningEventMapper.update(null, updateWrapper);
            if (affected == 0) {
                log.debug("AI 研判跳过, 该预警已被研判(条件更新影响行数为0): warningId={}", warningId);
                return;
            }
            log.info("AI 研判回填完成: warningId={}, eventCode={}, handlePriority={}",
                    warningId, current.getEventCode(), result.getHandlePriority());
        } catch (Exception e) {
            // 兜底：研判/回填任何异常仅告警，绝不影响预警主流程
            log.error("AI 研判回填异常, 已忽略: warningId={}", warningId, e);
        }
    }

    /**
     * 将研判结果拼装为结论摘要文本（超长截断，与列宽对齐）。
     *
     * @param result 研判结果
     * @return 结论摘要文本
     */
    private String buildResultSummary(AgentJudgeResultDTO result) {
        StringBuilder sb = new StringBuilder();
        appendPart(sb, "根因分析", result.getRootCause());
        appendPart(sb, "等级复核", result.getLevelReview());
        appendPart(sb, "影响范围", result.getImpactScope());
        appendPart(sb, "趋势预测", result.getTrendPrediction());
        appendPart(sb, "处置优先级", result.getHandlePriority());
        String summary = sb.toString();
        if (summary.isEmpty()) {
            return null;
        }
        return summary.length() > AI_RESULT_MAX_LENGTH
                ? summary.substring(0, AI_RESULT_MAX_LENGTH)
                : summary;
    }

    /**
     * 追加「标签:内容」片段（空值跳过）。
     *
     * @param sb    字符串构造器
     * @param label 片段标签
     * @param value 片段内容
     */
    private void appendPart(StringBuilder sb, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append("; ");
        }
        sb.append(label).append(": ").append(value.trim());
    }

    /**
     * 服务关闭时优雅停止研判线程池（等待在途研判至多 5 秒）。
     */
    @PreDestroy
    public void shutdown() {
        judgeExecutor.shutdown();
        try {
            if (!judgeExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                judgeExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            judgeExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

