import { get, post, put } from '@/utils/request'
import type { PageResult, PageQuery } from '@/types/api.d'
import type {
  PlanTemplate,
  PlanTemplateItem,
  PlanTemplateSaveRequest,
  PlanInstance,
  PlanInstanceVO,
  PlanDetailVO,
  PlanTaskVO,
  PlanTaskUpdateRequest,
  PlanGenerateRequest,
} from '@/types/plan.d'

/* ==================== 预案模板 ==================== */

/** 预案模板列表（兼容旧接口） */
export function getPlanTemplates(params: PageQuery & Record<string, unknown>): Promise<PageResult<PlanTemplate>> {
  return get('/api/v1/plans/templates', params as Record<string, unknown>)
}

/** 预案模板详情（兼容旧接口） */
export function getPlanTemplateById(planId: number): Promise<PlanTemplate> {
  return get(`/api/v1/plans/templates/${planId}`)
}

/** 创建预案模板（兼容旧接口） */
export function createPlanTemplate(data: Partial<PlanTemplate>): Promise<number> {
  return post('/api/v1/plans/templates', data)
}

/** 更新预案模板（兼容旧接口） */
export function updatePlanTemplate(planId: number, data: Partial<PlanTemplate>): Promise<void> {
  return put(`/api/v1/plans/templates/${planId}`, data)
}

/* ==================== 预案模板（新接口） ==================== */

/** 分页查询预案模板 */
export function fetchPlanTemplates(params: PageQuery & Record<string, unknown>): Promise<PageResult<PlanTemplateItem>> {
  return get('/api/v1/plans/templates', params as Record<string, unknown>)
}

/** 预案模板详情 */
export function fetchPlanTemplateDetail(templateId: number): Promise<PlanTemplateItem> {
  return get(`/api/v1/plans/templates/${templateId}`)
}

/** 创建预案模板 */
export function savePlanTemplate(data: PlanTemplateSaveRequest): Promise<PlanTemplateItem> {
  return post('/api/v1/plans/templates', data)
}

/** 更新预案模板 */
export function updatePlanTemplateById(templateId: number, data: Partial<PlanTemplateSaveRequest>): Promise<PlanTemplateItem> {
  return put(`/api/v1/plans/templates/${templateId}`, data)
}

/** 启用/停用预案模板 */
export function switchPlanTemplateStatus(templateId: number, status: number): Promise<PlanTemplateItem> {
  return put(`/api/v1/plans/templates/${templateId}/status`, null, { params: { status } })
}

/* ==================== 预案实例 ==================== */

/** 预案实例列表（兼容旧接口） */
export function getPlanInstances(params: PageQuery & Record<string, unknown>): Promise<PageResult<PlanInstance>> {
  return get('/api/v1/plans/instances', params as Record<string, unknown>)
}

/** 启动预案（兼容旧接口） */
export function startPlanInstance(planId: number, warningId: number): Promise<number> {
  return post('/api/v1/plans/instances', { planId, warningId })
}

/* ==================== 预案实例（新接口） ==================== */

/** 查询预案实例列表 */
export function fetchPlanInstances(params: PageQuery & Record<string, unknown>): Promise<PageResult<PlanInstanceVO>> {
  return get('/api/v1/plans/instances', params as Record<string, unknown>)
}

/** AI 生成预案 */
export function generatePlan(data: PlanGenerateRequest): Promise<PlanInstanceVO> {
  return post('/api/v1/plans/generate', data)
}

/** 预案实例详情（聚合） */
export function fetchPlanDetail(instanceId: number): Promise<PlanDetailVO> {
  return get(`/api/v1/plans/${instanceId}`)
}

/** 启动执行预案 */
export function executePlan(instanceId: number, dispatchOrder = false): Promise<PlanInstanceVO> {
  return post(`/api/v1/plans/${instanceId}/execute`, null, { params: { dispatchOrder } })
}

/** 查询实例任务清单 */
export function fetchPlanTasks(instanceId: number): Promise<PlanTaskVO[]> {
  return get(`/api/v1/plans/${instanceId}/tasks`)
}

/** 更新任务状态 */
export function updatePlanTask(taskId: number, data: PlanTaskUpdateRequest): Promise<PlanTaskVO> {
  return put(`/api/v1/plans/tasks/${taskId}`, data)
}

/** 三级联动上报 */
export function escalatePlan(instanceId: number): Promise<PlanInstanceVO> {
  return post(`/api/v1/plans/${instanceId}/escalate`)
}

/** 取消预案 */
export function cancelPlan(instanceId: number): Promise<PlanInstanceVO> {
  return post(`/api/v1/plans/${instanceId}/cancel`)
}

/** 归档预案 */
export function archivePlan(instanceId: number): Promise<PlanInstanceVO> {
  return post(`/api/v1/plans/${instanceId}/archive`)
}
