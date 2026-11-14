/** 传感器监测数据 —— 对应后端 SensorDataVO */
export interface SensorData {
  deviceId: string
  pipeType: string
  sensorType: string
  areaId: string
  ts: number
  pressure: number | null
  temperature: number | null
  flow: number | null
  vibration: number | null
  corrosion: number | null
  leakage: number | null
  /** 0=正常 1=异常 2=报警 */
  status: number
}

/** 传感器统计聚合数据 —— 对应后端 SensorStatisticsVO */
export interface SensorStatistics {
  deviceId: string
  ts: number
  count: number
  maxPressure: number | null
  minPressure: number | null
  avgPressure: number | null
  maxTemperature: number | null
  minTemperature: number | null
  avgTemperature: number | null
  maxFlow: number | null
  minFlow: number | null
  avgFlow: number | null
  maxVibration: number | null
  minVibration: number | null
  avgVibration: number | null
  maxCorrosion: number | null
  minCorrosion: number | null
  avgCorrosion: number | null
  maxLeakage: number | null
  minLeakage: number | null
  avgLeakage: number | null
}

/** 监测数据查询参数 */
export interface MonitorQuery {
  deviceId?: string
  startTime?: number
  endTime?: number
  interval?: string
  limit?: number
}

/** 统计查询参数 */
export interface StatisticsQuery {
  deviceId: string
  startTime: number
  endTime: number
  interval: string
}

/** WebSocket推送消息 */
export interface WsMonitorMessage {
  type: 'data' | 'warning' | 'device-status'
  payload: SensorData | Record<string, unknown>
  timestamp: number
}

/** 保留向后兼容别名 */
export type MonitorData = SensorData
