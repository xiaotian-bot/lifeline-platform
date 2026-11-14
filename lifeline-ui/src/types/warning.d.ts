/** 预警事件 */
export interface WarningEvent {
  warningId: number
  warningCode: string
  warningType: string
  level: string           // blue/yellow/orange/red
  source: string          // RULE/AI/MANUAL
  pipeId: number
  pipeType: string
  deviceId: number
  title: string
  description: string
  metricValue: number
  thresholdValue: number
  aiScore: number
  status: string          // pending/confirmed/handling/resolved/ignored
  createTime: string
  resolveTime: string
  handlerId: number
  handlerName: string
  reason: string
}

/** 预警规则 */
export interface WarningRule {
  ruleId: number
  ruleName: string
  ruleType: string        // PRESSURE_LOW/PRESSURE_HIGH/FLOW_HIGH/TEMP_HIGH/VIBRATION_HIGH
  metricType: string
  pipeType: string
  condition: string       // gt/lt/gte/lte/eq
  conditionExpr: string
  threshold: number
  thresholdValue: number
  level: string
  warningLevel: number    // 1-4
  duration: number        // 持续时间(秒)
  enabled: boolean
  createTime: string
}

/** 预报记录 */
export interface Forecast {
  forecastId: number
  forecastType: string
  areaId: number
  deviceName: string
  pipeName: string
  riskLevel: string
  trendDescription: string
  forecastTime: string
  riskTime: string
  confidence: number
  details: string
  createTime: string
}

/** 预警统计分组项 */
export interface StatisticsGroupItem {
  key: string
  label: string
  count: number
}

/** 预警统计 */
export interface WarningStatistics {
  total: number
  byLevel: StatisticsGroupItem[]
  byType: StatisticsGroupItem[]
  byStatus: StatisticsGroupItem[]
}
