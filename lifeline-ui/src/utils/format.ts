/** 格式化日期时间 */
export function formatDateTime(date: string | number | Date): string {
  if (!date) return '-'
  const d = new Date(date)
  if (isNaN(d.getTime())) return '-'
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

/** 格式化日期 */
export function formatDate(date: string | number | Date): string {
  if (!date) return '-'
  const d = new Date(date)
  if (isNaN(d.getTime())) return '-'
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

/** 格式化数字（千分位） */
export function formatNumber(num: number): string {
  if (num === null || num === undefined) return '-'
  return num.toLocaleString('zh-CN')
}

/** 预警等级文本 */
export function getLevelText(level: string): string {
  const map: Record<string, string> = {
    blue: '蓝色预警',
    yellow: '黄色预警',
    orange: '橙色预警',
    red: '红色预警',
  }
  return map[level] || level
}

/** 工单状态文本 */
export function getOrderStatusText(status: string): string {
  const map: Record<string, string> = {
    pending: '待处理',
    assigned: '已分配',
    'in-progress': '处理中',
    completed: '已完成',
    closed: '已关闭',
    rejected: '已驳回',
  }
  return map[status] || status
}
