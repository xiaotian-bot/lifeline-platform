import { get } from '@/utils/request'
import type { SensorData, SensorStatistics, MonitorQuery, StatisticsQuery } from '@/types/monitor.d'

/** 获取设备实时数据 */
export function getRealtimeData(deviceId: string): Promise<SensorData> {
  return get(`/api/v1/monitor/realtime/${deviceId}`)
}

/** 查询历史数据 */
export function getHistoryData(params: MonitorQuery): Promise<SensorData[]> {
  return get('/api/v1/monitor/history', params as unknown as Record<string, unknown>)
}

/** 查询统计聚合数据 */
export function getStatistics(params: StatisticsQuery): Promise<SensorStatistics[]> {
  return get('/api/v1/monitor/statistics', params as unknown as Record<string, unknown>)
}
