import { get, post, put } from '@/utils/request'
import type { PageResult, PageQuery } from '@/types/api.d'
import type {
  OperationOrder,
  OrderStatistics,
  InspectionPlan,
  OperationStaff,
  OperationMaterial,
} from '@/types/operation.d'

/* ======================== 工单接口 ======================== */

/** 工单列表 */
export function getOrders(params: PageQuery & Record<string, unknown>): Promise<PageResult<OperationOrder>> {
  return get('/api/v1/orders', params as Record<string, unknown>)
}

/** 工单统计 */
export function getOrderStatistics(): Promise<OrderStatistics> {
  return get('/api/v1/orders/statistics')
}

/** 工单详情 */
export function getOrderById(orderId: number): Promise<OperationOrder> {
  return get(`/api/v1/orders/${orderId}`)
}

/** 创建工单 */
export function createOrder(data: Partial<OperationOrder>): Promise<number> {
  return post('/api/v1/orders', data)
}

/** 更新工单 */
export function updateOrder(orderId: number, data: Partial<OperationOrder>): Promise<void> {
  return put(`/api/v1/orders/${orderId}`, data)
}

/** 接单 */
export function acceptOrder(orderId: number): Promise<OperationOrder> {
  return post(`/api/v1/orders/${orderId}/accept`)
}

/** 完工 */
export function completeOrder(orderId: number, result: string): Promise<OperationOrder> {
  return post(`/api/v1/orders/${orderId}/complete`, { result })
}

/** 关闭工单 */
export function closeOrder(orderId: number): Promise<OperationOrder> {
  return post(`/api/v1/orders/${orderId}/close`)
}

/* ======================== 巡检计划接口 ======================== */

/** 巡检计划列表 */
export function getInspectionPlans(params: PageQuery & Record<string, unknown>): Promise<PageResult<InspectionPlan>> {
  return get('/api/v1/inspections', params as Record<string, unknown>)
}

/** 巡检计划详情 */
export function getInspectionPlanById(planId: number): Promise<InspectionPlan> {
  return get(`/api/v1/inspections/${planId}`)
}

/** 创建巡检计划 */
export function createInspectionPlan(data: Partial<InspectionPlan>): Promise<number> {
  return post('/api/v1/inspections', data)
}

/** 巡检计划派生工单列表 */
export function getInspectionPlanTasks(planId: number): Promise<OperationOrder[]> {
  return get(`/api/v1/inspections/${planId}/tasks`)
}

/* ======================== 运维人员接口 ======================== */

/** 运维人员列表 */
export function getOperationStaff(params: PageQuery & Record<string, unknown>): Promise<PageResult<OperationStaff>> {
  return get('/api/v1/staff', params as Record<string, unknown>)
}

/** 新增运维人员 */
export function createStaff(data: Partial<OperationStaff>): Promise<number> {
  return post('/api/v1/staff', data)
}

/** 更新运维人员 */
export function updateStaff(staffId: number, data: Partial<OperationStaff>): Promise<void> {
  return put(`/api/v1/staff/${staffId}`, data)
}

/* ======================== 运维物资接口 ======================== */

/** 运维物资列表 */
export function getOperationMaterials(params: PageQuery & Record<string, unknown>): Promise<PageResult<OperationMaterial>> {
  return get('/api/v1/materials', params as Record<string, unknown>)
}

/** 新增物资 */
export function createMaterial(data: Partial<OperationMaterial>): Promise<number> {
  return post('/api/v1/materials', data)
}

/** 更新物资 */
export function updateMaterial(materialId: number, data: Partial<OperationMaterial>): Promise<void> {
  return put(`/api/v1/materials/${materialId}`, data)
}
