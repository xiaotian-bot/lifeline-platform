import { get, post, put, del } from '@/utils/request'
import type { PageResult, PageQuery } from '@/types/api.d'
import type { WarningEvent, WarningRule, Forecast, WarningStatistics } from '@/types/warning.d'

/** 预警事件列表 */
export function getWarnings(params: PageQuery & Record<string, unknown>): Promise<PageResult<WarningEvent>> {
  return get('/api/v1/warnings', params as Record<string, unknown>)
}

/** 预警详情 */
export function getWarningById(warningId: number): Promise<WarningEvent> {
  return get(`/api/v1/warnings/${warningId}`)
}

/** 确认预警 */
export function confirmWarning(warningId: number): Promise<void> {
  return post(`/api/v1/warnings/${warningId}/confirm`)
}

/** 核销预警 */
export function cancelWarning(warningId: number): Promise<void> {
  return post(`/api/v1/warnings/${warningId}/cancel`)
}

/** 预警统计 */
export function getWarningStatistics(): Promise<WarningStatistics> {
  return get('/api/v1/warnings/statistics')
}

/** 预警规则列表 */
export function getWarningRules(params?: PageQuery & Record<string, unknown>): Promise<PageResult<WarningRule>> {
  return get('/api/v1/warning-rules', params as Record<string, unknown>)
}

/** 创建预警规则 */
export function createWarningRule(data: Partial<WarningRule>): Promise<number> {
  return post('/api/v1/warning-rules', data)
}

/** 更新预警规则 */
export function updateWarningRule(ruleId: number, data: Partial<WarningRule>): Promise<void> {
  return put(`/api/v1/warning-rules/${ruleId}`, data)
}

/** 启用/停用预警规则 */
export function changeRuleStatus(ruleId: number, status: number): Promise<void> {
  return put(`/api/v1/warning-rules/${ruleId}/status?status=${status}`)
}

/** 删除预警规则 */
export function deleteWarningRule(ruleId: number): Promise<void> {
  return del(`/api/v1/warning-rules/${ruleId}`)
}

/** 预报列表 */
export function getForecasts(params: PageQuery & Record<string, unknown>): Promise<PageResult<Forecast>> {
  return get('/api/v1/forecasts', params as Record<string, unknown>)
}
