package com.tianxin.lifeline.warning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tianxin.lifeline.warning.entity.WarningRule;
import com.tianxin.lifeline.warning.mapper.WarningRuleMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 预警规则内存缓存服务。
 *
 * <p>规则判定桥在每条传感数据到达时做越限匹配，若逐条查库将成为性能瓶颈。
 * 本服务将启用状态（status=1）的规则全量加载进内存，
 * 启动时初始化并每 60 秒（可配置 lifeline.warning.rule-refresh-ms）定时刷新，
 * 规则 CRUD 变更最多延迟一个刷新周期生效。
 */
@Slf4j
@Service
public class RuleCacheService {

    /** 规则 Mapper */
    private final WarningRuleMapper warningRuleMapper;

    /** 启用规则内存快照（volatile 保证刷新对消费线程可见） */
    private volatile List<WarningRule> enabledRules = Collections.emptyList();

    /**
     * 构造缓存服务。
     *
     * @param warningRuleMapper 规则 Mapper
     */
    public RuleCacheService(WarningRuleMapper warningRuleMapper) {
        this.warningRuleMapper = warningRuleMapper;
    }

    /**
     * 启动时完成首次规则加载。
     */
    @PostConstruct
    public void init() {
        refresh();
    }

    /**
     * 定时刷新规则缓存（默认 60 秒一次）。
     */
    @Scheduled(fixedDelayString = "${lifeline.warning.rule-refresh-ms:60000}")
    public void refresh() {
        try {
            LambdaQueryWrapper<WarningRule> wrapper = new LambdaQueryWrapper<WarningRule>()
                    .eq(WarningRule::getStatus, WarningRule.STATUS_ENABLED);
            List<WarningRule> rules = warningRuleMapper.selectList(wrapper);
            this.enabledRules = Collections.unmodifiableList(rules);
            log.debug("预警规则缓存刷新完成, 启用规则数={}", rules.size());
        } catch (Exception e) {
            // 刷新失败保留旧快照，仅告警，不中断判定链路
            log.error("预警规则缓存刷新失败, 沿用旧快照", e);
        }
    }

    /**
     * 获取当前全部启用规则。
     *
     * @return 启用规则列表（不可变快照）
     */
    public List<WarningRule> enabledRules() {
        return enabledRules;
    }
}
