package com.tianxin.lifeline.aigateway.controller;

import com.tianxin.lifeline.aigateway.config.AiGatewayProperties;
import com.tianxin.lifeline.aigateway.dto.BatchDetectItemDTO;
import com.tianxin.lifeline.aigateway.dto.BatchDetectResultDTO;
import com.tianxin.lifeline.aigateway.service.AiCapabilityTemplate;
import com.tianxin.lifeline.aigateway.service.AiInvokeLogger;
import com.tianxin.lifeline.aigateway.service.ForecastReportPersister;
import com.tianxin.lifeline.api.ai.dto.AgentChatRequestDTO;
import com.tianxin.lifeline.api.ai.dto.AgentChatResultDTO;
import com.tianxin.lifeline.api.ai.dto.AgentJudgeRequestDTO;
import com.tianxin.lifeline.api.ai.dto.AgentJudgeResultDTO;
import com.tianxin.lifeline.api.ai.dto.AgentWorkflowResultDTO;
import com.tianxin.lifeline.api.ai.dto.AnomalyRequestDTO;
import com.tianxin.lifeline.api.ai.dto.AnomalyResultDTO;
import com.tianxin.lifeline.api.ai.dto.ForecastRequestDTO;
import com.tianxin.lifeline.api.ai.dto.ForecastResultDTO;
import com.tianxin.lifeline.api.ai.dto.PlanAgentRequestDTO;
import com.tianxin.lifeline.api.ai.dto.PlanAgentResultDTO;
import com.tianxin.lifeline.api.ai.dto.VisionAnalyzeRequestDTO;
import com.tianxin.lifeline.api.ai.dto.VisionAnalyzeResultDTO;
import com.tianxin.lifeline.common.core.result.ErrorCode;
import com.tianxin.lifeline.common.core.result.Result;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 能力网关控制器（前缀 /api/v1/ai，与统一网关路由 /api/v1/ai/** 一致）。
 *
 * <p>端点清单：
 * <ul>
 *     <li>POST /anomaly-detect：时序异常检测（转发 ml-service）</li>
 *     <li>POST /forecast：趋势预报（转发 ml-service，非降级结果直写 forecast_report）</li>
 *     <li>POST /agent/judge、/agent/plan、/agent/chat、/agent/workflow：转发 agent-service</li>
 *     <li>POST /vision/analyze：视觉分析（P9 后接入，当前返回降级结果）</li>
 *     <li>POST /batch-detect：批量异常检测（&gt;250 条切分 4 路并发子批）</li>
 *     <li>GET /health：上游连通性自检</li>
 * </ul>
 *
 * <p>所有端点均不抛 5xx：上游失败统一返回结构化降级结果（HTTP 200）。
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AiGatewayController {

    /** ml-service 上游路径：时序异常检测 */
    private static final String ML_ANOMALY_DETECT_URI = "/api/v1/ai/anomaly-detect";

    /** ml-service 上游路径：趋势预报 */
    private static final String ML_FORECAST_URI = "/api/v1/ai/forecast";

    /** 批量检测切分阈值：超过该条数时切分为 4 路并发子批 */
    private static final int BATCH_SPLIT_THRESHOLD = 250;

    /** 批量检测并发路数 */
    private static final int BATCH_CONCURRENCY = 4;

    private final WebClient mlWebClient;
    private final WebClient agentWebClient;
    private final WebClient healthWebClient;
    private final AiCapabilityTemplate template;
    private final ForecastReportPersister forecastReportPersister;
    private final AiInvokeLogger invokeLogger;
    private final AiGatewayProperties properties;

    /**
     * 构造控制器。
     *
     * @param mlWebClient            ml-service 上游客户端
     * @param agentWebClient         agent-service 上游客户端
     * @param healthWebClient        连通性自检客户端
     * @param template               AI 能力调用模板
     * @param forecastReportPersister forecast 落库服务
     * @param invokeLogger           调用日志器
     * @param properties             AI 网关配置
     */
    public AiGatewayController(@Qualifier("mlWebClient") WebClient mlWebClient,
                               @Qualifier("agentWebClient") WebClient agentWebClient,
                               @Qualifier("healthWebClient") WebClient healthWebClient,
                               AiCapabilityTemplate template,
                               ForecastReportPersister forecastReportPersister,
                               AiInvokeLogger invokeLogger,
                               AiGatewayProperties properties) {
        this.mlWebClient = mlWebClient;
        this.agentWebClient = agentWebClient;
        this.healthWebClient = healthWebClient;
        this.template = template;
        this.forecastReportPersister = forecastReportPersister;
        this.invokeLogger = invokeLogger;
        this.properties = properties;
    }

    /**
     * 时序异常检测（转发 ml-service）。
     *
     * @param request 异常检测请求
     * @return 异常检测结果（上游失败时降级：data=null）
     */
    @PostMapping("/anomaly-detect")
    public Mono<Result<AnomalyResultDTO>> anomalyDetect(@RequestBody AnomalyRequestDTO request) {
        return template.invoke("anomaly-detect", mlWebClient, ML_ANOMALY_DETECT_URI,
                request, AnomalyResultDTO.class,
                properties.getTimeout().getDetectMs(), true);
    }

    /**
     * 趋势预报（转发 ml-service）。
     *
     * <p>非降级、非缓存命中的结果由网关直写 forecast_report（失败仅 warn）。
     *
     * @param request 预报请求
     * @return 预报结果（上游失败时降级：data=null）
     */
    @PostMapping("/forecast")
    public Mono<Result<ForecastResultDTO>> forecast(@RequestBody ForecastRequestDTO request) {
        return template.invokeRaw("forecast", mlWebClient, ML_FORECAST_URI,
                        request, ForecastResultDTO.class,
                        properties.getTimeout().getForecastMs(), true)
                .map(outcome -> {
                    // 落库决策：仅上游真实返回的新结果落库；降级结果与缓存命中不重复写入
                    if (!outcome.degraded() && !outcome.cacheHit() && outcome.result().getData() != null) {
                        forecastReportPersister.persistAsync(request, outcome.result().getData());
                    }
                    return outcome.result();
                });
    }

    /**
     * 预警深度研判（转发 agent-service /agent/judge）。
     *
     * @param request 研判请求
     * @return 研判结果
     */
    @PostMapping("/agent/judge")
    public Mono<Result<AgentJudgeResultDTO>> agentJudge(@RequestBody AgentJudgeRequestDTO request) {
        return template.invoke("agent-judge", agentWebClient, "/api/v1/ai/agent/judge",
                request, AgentJudgeResultDTO.class,
                properties.getTimeout().getAgentMs(), true);
    }

    /**
     * 智能预案生成（转发 agent-service /agent/plan）。
     *
     * <p>请求/响应沿用既有 PlanAgentRequestDTO/PlanAgentResultDTO 契约。
     *
     * @param request 预案生成请求
     * @return 预案生成结果
     */
    @PostMapping("/agent/plan")
    public Mono<Result<PlanAgentResultDTO>> agentPlan(@RequestBody PlanAgentRequestDTO request) {
        return template.invoke("agent-plan", agentWebClient, "/api/v1/ai/agent/plan",
                request, PlanAgentResultDTO.class,
                properties.getTimeout().getAgentMs(), true);
    }

    /**
     * 管网安全运维问答（转发 agent-service /agent/chat）。
     *
     * @param request 问答请求
     * @return 问答结果
     */
    @PostMapping("/agent/chat")
    public Mono<Result<AgentChatResultDTO>> agentChat(@RequestBody AgentChatRequestDTO request) {
        return template.invoke("agent-chat", agentWebClient, "/api/v1/ai/agent/chat",
                request, AgentChatResultDTO.class,
                properties.getTimeout().getAgentMs(), true);
    }

    /**
     * 研判-预案一体化工作流（转发 agent-service /agent/workflow）。
     *
     * @param request 研判请求（入参同 /agent/judge）
     * @return judge + plan 合并结果
     */
    @PostMapping("/agent/workflow")
    public Mono<Result<AgentWorkflowResultDTO>> agentWorkflow(@RequestBody AgentJudgeRequestDTO request) {
        return template.invoke("agent-workflow", agentWebClient, "/api/v1/ai/agent/workflow",
                request, AgentWorkflowResultDTO.class,
                properties.getTimeout().getAgentMs(), true);
    }

    /**
     * 施工影像视觉分析（降级端点）。
     *
     * <p>当前无视觉服务（计划 P9 后接入），直接返回 violation=false 的
     * 结构化降级结果，对齐 VisionAnalyzeResultDTO 契约，避免调用方阻塞。
     *
     * @param request 视觉分析请求
     * @return 降级的视觉分析结果
     */
    @PostMapping("/vision/analyze")
    public Mono<Result<VisionAnalyzeResultDTO>> visionAnalyze(@RequestBody VisionAnalyzeRequestDTO request) {
        long startMs = System.currentTimeMillis();
        VisionAnalyzeResultDTO data = new VisionAnalyzeResultDTO();
        data.setViolation(false);
        data.setConfidence(0.0);
        data.setDescription("视觉分析能力计划于P9阶段接入，当前无视觉服务，返回降级结果（无违规）");

        Result<VisionAnalyzeResultDTO> result = new Result<>();
        result.setCode(Result.SUCCESS_CODE);
        result.setMessage(AiCapabilityTemplate.DEGRADED_MESSAGE_PREFIX
                + "视觉分析能力P9阶段接入，当前返回默认无违规结果");
        result.setData(data);
        invokeLogger.log("vision-analyze", System.currentTimeMillis() - startMs, true, false);
        return Mono.just(result);
    }

    /**
     * 批量异常检测。
     *
     * <p>请求体为多设备检测请求数组；超过 250 条时切分为 4 路子批
     * 并发执行（flatMapSequential 限并发为 4，且按子批原顺序汇总），
     * 结果与请求顺序一一对应。
     *
     * @param requests 异常检测请求列表
     * @return 汇总结果（含逐条成功/降级标记）
     */
    @PostMapping("/batch-detect")
    public Mono<Result<BatchDetectResultDTO>> batchDetect(@RequestBody List<AnomalyRequestDTO> requests) {
        if (requests == null || requests.isEmpty()) {
            return Mono.just(Result.fail(ErrorCode.PARAM_INVALID.getCode(), "批量检测请求列表不能为空"));
        }
        // >250 条切分为 4 路子批并发；否则单批顺序执行
        List<List<AnomalyRequestDTO>> groups = requests.size() > BATCH_SPLIT_THRESHOLD
                ? partition(requests, BATCH_CONCURRENCY)
                : List.of(requests);

        return Flux.fromIterable(groups)
                .flatMapSequential(this::processGroup, Math.min(groups.size(), BATCH_CONCURRENCY))
                .collectList()
                .map(groupResults -> {
                    List<BatchDetectItemDTO> items = new ArrayList<>();
                    groupResults.forEach(items::addAll);
                    int successCount = (int) items.stream()
                            .filter(item -> Boolean.TRUE.equals(item.getSuccess())).count();

                    BatchDetectResultDTO data = new BatchDetectResultDTO();
                    data.setTotal(items.size());
                    data.setSuccessCount(successCount);
                    data.setDegradedCount(items.size() - successCount);
                    data.setItems(items);
                    return Result.success(data);
                });
    }

    /**
     * 上游连通性自检：返回 ml-service / agent-service 各自可达性。
     *
     * <p>探活方式：GET 上游 /docs（FastAPI 默认端点），任意 HTTP 响应
     * 即视为可达；连接失败或超时（2s）视为不可达。
     *
     * @return 自检结果
     */
    @GetMapping("/health")
    public Mono<Result<Map<String, Object>>> health() {
        Mono<Boolean> mlProbe = probe(properties.getMlService().getUrl());
        Mono<Boolean> agentProbe = probe(properties.getAgentService().getUrl());
        return Mono.zip(mlProbe, agentProbe).map(tuple -> {
            Map<String, Object> data = new LinkedHashMap<>();
            Map<String, Object> ml = new LinkedHashMap<>();
            ml.put("url", properties.getMlService().getUrl());
            ml.put("reachable", tuple.getT1());
            Map<String, Object> agent = new LinkedHashMap<>();
            agent.put("url", properties.getAgentService().getUrl());
            agent.put("reachable", tuple.getT2());
            data.put("mlService", ml);
            data.put("agentService", agent);
            data.put("checkedAt", System.currentTimeMillis());
            return Result.success(data);
        });
    }

    /**
     * 处理一个子批（批内顺序执行，保持结果顺序）。
     *
     * @param group 子批请求
     * @return 子批结果列表
     */
    private Mono<List<BatchDetectItemDTO>> processGroup(List<AnomalyRequestDTO> group) {
        return Flux.fromIterable(group)
                .concatMap(this::detectOne)
                .collectList();
    }

    /**
     * 批量检测中的单条检测（复用检测能力，单条失败不影响其他条目）。
     *
     * @param request 检测请求
     * @return 单条结果项
     */
    private Mono<BatchDetectItemDTO> detectOne(AnomalyRequestDTO request) {
        return template.invokeRaw("batch-detect", mlWebClient, ML_ANOMALY_DETECT_URI,
                        request, AnomalyResultDTO.class,
                        properties.getTimeout().getDetectMs(), true)
                .map(outcome -> {
                    BatchDetectItemDTO item = new BatchDetectItemDTO();
                    boolean success = !outcome.degraded() && outcome.result().getData() != null;
                    item.setSuccess(success);
                    item.setMessage(outcome.result().getMessage());
                    item.setData(outcome.result().getData());
                    return item;
                });
    }

    /**
     * 探活单个上游服务。
     *
     * @param baseUrl 上游基础地址
     * @return 是否可达
     */
    private Mono<Boolean> probe(String baseUrl) {
        return healthWebClient.get()
                .uri(baseUrl + "/docs")
                .exchangeToMono(response -> response.releaseBody().thenReturn(Boolean.TRUE))
                .timeout(Duration.ofMillis(properties.getTimeout().getHealthMs()))
                .onErrorReturn(Boolean.FALSE);
    }

    /**
     * 将列表近似均分为 parts 个子批。
     *
     * @param list  原列表
     * @param parts 子批数量
     * @param <T>   元素类型
     * @return 子批列表
     */
    private <T> List<List<T>> partition(List<T> list, int parts) {
        int chunkSize = (list.size() + parts - 1) / parts;
        List<List<T>> groups = new ArrayList<>();
        for (int i = 0; i < list.size(); i += chunkSize) {
            groups.add(list.subList(i, Math.min(list.size(), i + chunkSize)));
        }
        return groups;
    }
}
