import { get, post } from '@/utils/request'
import { createLongRequest } from '@/utils/request'
import type {
  AnomalyRequest, AnomalyResult,
  ForecastRequest, ForecastResult,
  AgentJudgeRequest, AgentJudgeResult,
  AgentChatRequest, AgentChatResult,
  PlanAgentRequest, PlanAgentResult,
  VisionAnalyzeRequest, VisionAnalyzeResult,
  AgentWorkflowResult,
} from '@/types/ai.d'

const longRequest = createLongRequest()

/** AI诊断 */
export function aiDiagnose(data: Record<string, unknown>): Promise<Record<string, unknown>> {
  return post('/api/v1/ai/diagnose', data)
}

/** AI风险分析 */
export function aiRiskAnalysis(data: Record<string, unknown>): Promise<Record<string, unknown>> {
  return post('/api/v1/ai/risk-analysis', data)
}

/** AI报告生成（长请求） */
export function aiGenerateReport(data: Record<string, unknown>): Promise<string> {
  return longRequest.post('/api/v1/ai/generate-report', data).then((res) => res.data.data)
}

/** 获取AI模型列表 */
export function getAiModels(): Promise<Record<string, unknown>[]> {
  return get('/api/v1/ai/models')
}

/** 时序异常检测 */
export function anomalyDetect(data: AnomalyRequest): Promise<AnomalyResult | null> {
  return post('/api/v1/ai/anomaly-detect', data)
}

/** 趋势预报 */
export function forecast(data: ForecastRequest): Promise<ForecastResult | null> {
  return post('/api/v1/ai/forecast', data)
}

/** AI预警研判 */
export function agentJudge(data: AgentJudgeRequest): Promise<AgentJudgeResult | null> {
  return post('/api/v1/ai/agent/judge', data)
}

/** AI预案生成 */
export function agentPlan(data: PlanAgentRequest): Promise<PlanAgentResult | null> {
  return post('/api/v1/ai/agent/plan', data)
}

/** AI运维问答 */
export function agentChat(data: AgentChatRequest): Promise<AgentChatResult | null> {
  return post('/api/v1/ai/agent/chat', data)
}

/** AI研判+预案一体化工作流 */
export function agentWorkflow(data: AgentJudgeRequest): Promise<AgentWorkflowResult | null> {
  return post('/api/v1/ai/agent/workflow', data)
}

/** 视觉分析 */
export function visionAnalyze(data: VisionAnalyzeRequest): Promise<VisionAnalyzeResult | null> {
  return post('/api/v1/ai/vision/analyze', data)
}

/** AI上游健康检查 */
export function aiHealth(): Promise<Record<string, unknown>> {
  return get('/api/v1/ai/health')
}
