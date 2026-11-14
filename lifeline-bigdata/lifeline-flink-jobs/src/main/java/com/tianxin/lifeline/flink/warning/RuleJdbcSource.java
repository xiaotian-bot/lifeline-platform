package com.tianxin.lifeline.flink.warning;

import com.tianxin.lifeline.flink.model.WarningRuleModel;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 预警规则 JDBC 定时源。
 *
 * <p>每 {@code rule-refresh-ms}（默认 60s）全量读取 PostgreSQL warning_rule 表
 * 启用规则（status=1 AND deleted=0），作为规则快照向下游广播，
 * 实现规则热更新（与 warning-service RuleCacheService 的定时刷新语义对齐）。
 *
 * <p>并行度固定为 1（规则源无需并行）；读取失败仅告警并沿用上一快照节奏重试，
 * 不中断作业。
 */
public class RuleJdbcSource extends RichSourceFunction<List<WarningRuleModel>> {

    private static final Logger log = LoggerFactory.getLogger(RuleJdbcSource.class);

    /** 启用规则查询 SQL（status=1 且未删除） */
    private static final String QUERY_SQL =
            "SELECT id, rule_name, rule_type, pipe_type, condition_expr, threshold_value, warning_level "
                    + "FROM warning_rule WHERE status = 1 AND deleted = 0";

    /** PostgreSQL JDBC URL */
    private final String url;

    /** PostgreSQL 用户名 */
    private final String username;

    /** PostgreSQL 密码 */
    private final String password;

    /** 刷新间隔（毫秒） */
    private final long refreshMs;

    /** 运行标记 */
    private volatile boolean running = true;

    /**
     * 构造规则源。
     *
     * @param url       PostgreSQL JDBC URL
     * @param username  用户名
     * @param password  密码
     * @param refreshMs 刷新间隔毫秒
     */
    public RuleJdbcSource(String url, String username, String password, long refreshMs) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.refreshMs = Math.max(1_000L, refreshMs);
    }

    @Override
    public void run(SourceContext<List<WarningRuleModel>> ctx) throws Exception {
        while (running) {
            List<WarningRuleModel> rules = loadRules();
            if (rules != null) {
                synchronized (ctx.getCheckpointLock()) {
                    ctx.collect(rules);
                }
                log.info("预警规则快照已刷新: {} 条启用规则", rules.size());
            }
            Thread.sleep(refreshMs);
        }
    }

    @Override
    public void cancel() {
        running = false;
    }

    /**
     * 全量读取启用规则。
     *
     * @return 规则列表，读取失败返回 null（本轮不刷新）
     */
    private List<WarningRuleModel> loadRules() {
        try (Connection connection = DriverManager.getConnection(url, username, password);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(QUERY_SQL)) {
            List<WarningRuleModel> rules = new ArrayList<>();
            while (resultSet.next()) {
                WarningRuleModel rule = new WarningRuleModel();
                rule.setId(resultSet.getLong("id"));
                rule.setRuleName(resultSet.getString("rule_name"));
                rule.setRuleType(resultSet.getString("rule_type"));
                rule.setPipeType(resultSet.getString("pipe_type"));
                rule.setConditionExpr(resultSet.getString("condition_expr"));
                double threshold = resultSet.getDouble("threshold_value");
                rule.setThresholdValue(resultSet.wasNull() ? null : threshold);
                int level = resultSet.getInt("warning_level");
                rule.setWarningLevel(resultSet.wasNull() ? null : level);
                rules.add(rule);
            }
            return rules;
        } catch (Exception e) {
            log.error("预警规则读取失败, 本轮沿用旧快照", e);
            return null;
        }
    }
}
