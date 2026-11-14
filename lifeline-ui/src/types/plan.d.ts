/** 预案模板 */
export interface PlanTemplate {
  planId: number
  planCode: string
  planName: string
  planType: string
  warningType: string
  level: string
  content: string
  steps: PlanStep[]
  status: string          // draft/active/disabled
  version: number
  createTime: string
  updateTime: string
}

/** 预案模板（对齐后端 PlanTemplateSaveRequest / PlanTemplateQueryRequest） */
export interface PlanTemplateItem {
  id: number
  templateName: string
  templateType: string
  scenarioType: string
  content: string
  status: number           // 1启用 / 0停用
  createdAt: string
  updatedAt: string
}

/** 预案步骤 */
export interface PlanStep {
  stepOrder: number
  stepName: string
  description: string
  responsibleRole: string
  estimatedTime: number   // 分钟
}

/** 预案实例 */
export interface PlanInstance {
  instanceId: number
  planId: number
  planName: string
  warningId: number
  status: string          // pending/executing/completed/cancelled
  startTime: string
  endTime: string
  executorId: number
  currentStep: number
  createTime: string
}

/** 预案实例VO（对齐后端 PlanInstanceVO） */
export interface PlanInstanceVO {
  id: number
  instanceCode: string
  planName: string
  templateId: number
  templateName: string
  scenarioType: string
  warningEventId: number
  status: number           // 0草稿/1执行中/2已完成/3已归档/4已取消
  statusText: string
  source: string           // AI / TEMPLATE
  generatedBy: number
  suggestion: string
  escalationLevel: number  // 0企业/1主管/2应急
  startTime: string
  endTime: string
  createdAt: string
  taskTotal: number
  taskCompleted: number
}

/** 预案任务VO（对齐后端 PlanTaskVO） */
export interface PlanTaskVO {
  id: number
  instanceId: number
  taskName: string
  taskType: string
  assignee: string
  status: number           // 0待执行/1执行中/2已完成/3已取消
  statusText: string
  priority: number         // 1高/2中/3低
  startTime: string
  endTime: string
  createdAt: string
}

/** 预案资源VO */
export interface PlanResourceVO {
  id: number
  instanceId: number
  resourceType: string     // MATERIAL/EQUIPMENT/STAFF
  resourceName: string
  quantity: number
  location: string
  status: number           // 0待调配/1已调配/2已到位
}

/** 预案日志VO */
export interface PlanLogVO {
  id: number
  instanceId: number
  action: string
  operator: string
  content: string
  logTime: string
}

/** 预案详情VO（聚合） */
export interface PlanDetailVO {
  instance: PlanInstanceVO
  tasks: PlanTaskVO[]
  resources: PlanResourceVO[]
  logs: PlanLogVO[]
}

/** 预案生成请求 */
export interface PlanGenerateRequest {
  warningEventId: number
  scenarioType?: string
  templateId?: number
  locationDesc?: string
  pipeType?: string
  warningLevel?: number
  metricType?: string
  title?: string
  description?: string
}

/** 预案模板保存请求 */
export interface PlanTemplateSaveRequest {
  templateName: string
  templateType?: string
  scenarioType?: string
  content: string
  status?: number
}

/** 任务状态更新请求 */
export interface PlanTaskUpdateRequest {
  status: number           // 1执行中/2已完成/3已取消
  assignee?: string
  remark?: string
}
