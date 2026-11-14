/** 统一响应体 - 匹配后端 Result.java */
export interface ApiResult<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

/** 分页响应 - 匹配后端分页约定 */
export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

/** 分页查询参数 */
export interface PageQuery {
  page?: number   // 从1开始
  size?: number   // 默认20，最大500
  orderBy?: string
  orderDir?: 'asc' | 'desc'
}
