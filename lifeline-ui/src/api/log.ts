import { get } from '@/utils/request'
import type { PageResult, PageQuery } from '@/types/api.d'
import type { OperationLog } from '@/types/auth.d'

/** 操作日志列表（分页） */
export function getOperationLogs(params: PageQuery & Record<string, unknown>): Promise<PageResult<OperationLog>> {
  return get('/api/v1/auth/logs', params as Record<string, unknown>)
}
