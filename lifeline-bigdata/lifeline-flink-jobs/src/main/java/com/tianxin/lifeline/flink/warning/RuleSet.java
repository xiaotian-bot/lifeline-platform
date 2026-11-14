package com.tianxin.lifeline.flink.warning;

import com.tianxin.lifeline.flink.model.SensorMessage;
import com.tianxin.lifeline.flink.model.WarningRuleModel;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 启用规则快照（广播状态载体）。
 *
 * <p>规则语义与 warning-service RuleJudgeServiceImpl 对齐并增强：
 * <ul>
 *     <li>取值指标与比较符以 condition_expr 解析为准，支持
 *         {@code < / <= / > / >=} 四种比较符（种子规则含
 *         "pressure < threshold" 低压语义，也兼容 ">=" 高压语义）；</li>
 *     <li>pipe_type 为空表示不限管网类型，非空大小写不敏感匹配，
 *         并做别名归一化（heating→HEAT、drainage→SEWAGE）；</li>
 *     <li>多条规则命中时取风险最高（warning_level 数值最小）的一条，
 *         与 RuleJudgeServiceImpl.higherSeverity 一致。</li>
 * </ul>
 */
public class RuleSet implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** condition_expr 语法：{指标} {比较符} threshold */
    private static final Pattern CONDITION_PATTERN = Pattern.compile(
            "^\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*(<=|>=|<|>)\\s*threshold\\s*$",
            Pattern.CASE_INSENSITIVE);

    /** 种子数据管网类型别名 → PipeType 标准编码 */
    private static final Map<String, String> PIPE_TYPE_ALIAS = Map.of(
            "heating", "HEAT",
            "drainage", "SEWAGE");

    /** 规则列表 */
    private final List<WarningRuleModel> rules;

    /** 惰性构建的解析结果 */
    private transient List<ParsedRule> parsedCache;

    /**
     * 构造规则快照。
     *
     * @param rules 启用规则列表（可为 null）
     */
    public RuleSet(List<WarningRuleModel> rules) {
        this.rules = rules == null ? new ArrayList<>() : rules;
    }

    /**
     * 获取解析后的规则列表（condition_expr 非法的规则跳过）。
     *
     * @return 可判定规则列表
     */
    public List<ParsedRule> parsedRules() {
        if (parsedCache == null) {
            parsedCache = new ArrayList<>();
            for (WarningRuleModel rule : rules) {
                ParsedRule parsed = parse(rule);
                if (parsed != null) {
                    parsedCache.add(parsed);
                }
            }
        }
        return parsedCache;
    }

    /**
     * 解析单条规则的 condition_expr。
     *
     * @param rule 规则
     * @return 解析结果，表达式非法或阈值缺失返回 null
     */
    private static ParsedRule parse(WarningRuleModel rule) {
        if (rule == null || rule.getConditionExpr() == null || rule.getThresholdValue() == null) {
            return null;
        }
        Matcher matcher = CONDITION_PATTERN.matcher(rule.getConditionExpr());
        if (!matcher.matches()) {
            return null;
        }
        return new ParsedRule(rule, matcher.group(1).toLowerCase(Locale.ROOT), matcher.group(2));
    }

    /**
     * 对单条传感数据做全量规则判定。
     *
     * @param message 传感数据
     * @return 命中的最高风险规则与触发值，未命中返回 null
     */
    public Hit evaluate(SensorMessage message) {
        ParsedRule hitRule = null;
        Double hitValue = null;
        for (ParsedRule parsed : parsedRules()) {
            if (!matchPipeType(parsed.getRule().getPipeType(), message.getPipeType())) {
                continue;
            }
            Double value = message.metricValue(parsed.getMetric());
            if (value == null) {
                continue;
            }
            if (!parsed.exceeded(value)) {
                continue;
            }
            if (hitRule == null || higherSeverity(parsed.getRule(), hitRule.getRule())) {
                hitRule = parsed;
                hitValue = value;
            }
        }
        return hitRule == null ? null : new Hit(hitRule, hitValue);
    }

    /**
     * 管网类型匹配：规则未限定视为全匹配；否则别名归一化后大小写不敏感比较。
     *
     * @param rulePipeType   规则限定管网类型
     * @param messagePipeType 消息管网类型
     * @return true 表示匹配
     */
    private static boolean matchPipeType(String rulePipeType, String messagePipeType) {
        if (rulePipeType == null || rulePipeType.isBlank()) {
            return true;
        }
        String normalized = normalizePipeType(rulePipeType.trim());
        return normalized.equalsIgnoreCase(
                messagePipeType == null ? "" : messagePipeType.trim());
    }

    /**
     * 管网类型别名归一化（heating→HEAT、drainage→SEWAGE，其余大写原样）。
     *
     * @param pipeType 原始值
     * @return 归一化编码
     */
    private static String normalizePipeType(String pipeType) {
        String lower = pipeType.toLowerCase(Locale.ROOT);
        return PIPE_TYPE_ALIAS.getOrDefault(lower, pipeType.toUpperCase(Locale.ROOT));
    }

    /**
     * 风险更高判定：warning_level 数值更小 = 风险更高（与 RuleJudgeServiceImpl 一致）。
     *
     * @param candidate 候选规则
     * @param current   当前命中规则
     * @return true 表示候选风险更高
     */
    private static boolean higherSeverity(WarningRuleModel candidate, WarningRuleModel current) {
        Integer cl = candidate.getWarningLevel();
        Integer ul = current.getWarningLevel();
        if (cl == null) {
            return false;
        }
        if (ul == null) {
            return true;
        }
        return cl < ul;
    }

    /**
     * 解析后的规则（指标 + 比较符 + 阈值）。
     */
    public static class ParsedRule implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /** 原始规则 */
        private final WarningRuleModel rule;

        /** 取值指标（小写：pressure/flow 等） */
        private final String metric;

        /** 比较符：< / <= / > / >= */
        private final String operator;

        ParsedRule(WarningRuleModel rule, String metric, String operator) {
            this.rule = rule;
            this.metric = metric;
            this.operator = operator;
        }

        public WarningRuleModel getRule() {
            return rule;
        }

        public String getMetric() {
            return metric;
        }

        public String getOperator() {
            return operator;
        }

        /**
         * 越限判定。
         *
         * @param value 实测值
         * @return true 表示命中
         */
        boolean exceeded(double value) {
            double threshold = rule.getThresholdValue();
            return switch (operator) {
                case "<" -> value < threshold;
                case "<=" -> value <= threshold;
                case ">" -> value > threshold;
                case ">=" -> value >= threshold;
                default -> false;
            };
        }
    }

    /**
     * 判定命中结果。
     */
    public static class Hit implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /** 命中规则 */
        private final ParsedRule parsedRule;

        /** 触发值 */
        private final Double value;

        Hit(ParsedRule parsedRule, Double value) {
            this.parsedRule = parsedRule;
            this.value = value;
        }

        public ParsedRule getParsedRule() {
            return parsedRule;
        }

        public Double getValue() {
            return value;
        }
    }
}
