/** 异常检测请求 */
export interface AnomalyRequest {
  deviceId?: number
  deviceCode?: string
  pipeType?: string
  metricType?: string
  series: number[]
  startTime?: number
  interval?: number
  model?: string
}

/** 异常检测结果 */
export interface AnomalyResult {
  deviceId: number | null
  metricType: string | null
  anomaly: boolean
  anomalyScore: number
  anomalyType: string | null
  confidence: number
  anomalyIndexes: number[]
  model: string | null
  costMs: number | null
}

/** 预报序列点 */
export interface ForecastPoint {
  time: string
  value: number
}

/** 趋势预报请求 */
export interface ForecastRequest {
  deviceCode?: string
  pipeType?: string
  metricType?: string
  series: number[]
  horizonDays?: number
}

/** 趋势预报结果 */
export interface ForecastResult {
  forecastType: string | null
  trendDesc: string | null
  riskTime: string | null
  confidence: number | null
  reportContent: string | null
  forecastSeries: ForecastPoint[] | null
}

/** AI Agent 研判请求 */
export interface AgentJudgeRequest {
  warningId?: number
  pipeType?: string
  warningLevel?: number
  metricType?: string
  triggerValue?: number
  threshold?: number
  title?: string
  description?: string
}

/** AI Agent 研判结果 */
export interface AgentJudgeResult {
  rootCause: string | null
  levelReview: string | null
  impactScope: string | null
  trendPrediction: string | null
  handlePriority: string | null
  confidence: number | null
}

/** AI Agent 问答请求 */
export interface AgentChatRequest {
  question: string
  context?: string
}

/** AI Agent 问答结果 */
export interface AgentChatResult {
  answer: string | null
  references: string[] | null
}

/** AI Agent 预案请求 */
export interface PlanAgentRequest {
  warningId?: number
  pipeType?: string
  warningLevel?: number
  metricType?: string
  title?: string
  description?: string
}

/** AI 预案任务项 */
export interface PlanTaskItem {
  taskName: string
  taskOrder: number
  description: string | null
}

/** AI 预案资源项 */
export interface PlanResourceItem {
  resourceName: string
  quantity: string
  unit: string | null
}

/** AI Agent 预案结果 */
export interface PlanAgentResult {
  planName: string | null
  tasks: PlanTaskItem[] | null
  resources: PlanResourceItem[] | null
  suggestion: string | null
}

/** 视觉分析请求 */
export interface VisionAnalyzeRequest {
  cameraId?: string
  projectId?: number
  mediaUrl?: string
  mediaType?: string
}

/** 视觉分析结果 */
export interface VisionAnalyzeResult {
  violation: boolean
  violationType: string | null
  confidence: number
  evidenceUrl: string | null
  description: string | null
}

/** AI 工作流结果（研判+预案一体化） */
export interface AgentWorkflowResult {
  judge: AgentJudgeResult | null
  plan: PlanAgentResult | null
}
