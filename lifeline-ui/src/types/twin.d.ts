/** 数字孪生场景 */
export interface TwinScene {
  sceneId: number
  sceneName: string
  sceneType: string
  areaId: number
  modelUrl: string
  thumbnail: string
  bounds: {
    sw: [number, number]
    ne: [number, number]
  }
  layers: TwinLayer[]
  createTime: string
}

/** 数字孪生图层 */
export interface TwinLayer {
  layerId: number
  layerName: string
  layerType: string       // pipe/device/warning/construction
  visible: boolean
  opacity: number
  dataUrl: string
}

/** 孪生数据点 */
export interface TwinDataPoint {
  id: number
  lng: number
  lat: number
  altitude: number
  properties: Record<string, unknown>
  style: Record<string, unknown>
}

/** 模型数据聚合 */
export interface ModelDataVO {
  pipeType: string | null
  areaId: number | null
  pipeCount: number
  deviceCount: number
  manholeCount: number
  totalLengthMeters: number
  geoFeatureCount: number
  geoFeatureSummary: Record<string, unknown>[]
  degraded: boolean
  degradedSources: string[]
}

/** 设备实时状态 */
export interface DeviceRealtimeVO {
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
  status: number | null   // 0 正常 / 1 异常 / 2 报警
}

/** 实时映射请求 */
export interface RealtimeMappingRequest {
  deviceIds?: string[]
  areaId?: number
}

/** 实时映射结果 */
export interface RealtimeMappingVO {
  total: number
  matched: number
  missed: number
  pages: number
  devices: DeviceRealtimeVO[]
  degraded: boolean
  degradedSources: string[]
}

/** 预警定位条目 */
export interface WarningLocationItemVO {
  warningId: number
  warningNo: string
  warningLevel: string
  title: string
  status: string
  pipeType: string
  pipeId: number | null
  pipeName: string | null
  deviceId: number | null
  deviceCode: string | null
  longitude: number | null
  latitude: number | null
  locationSource: string
  realtimeStatus: number | null
  realtimeTs: number | null
}

/** 预警定位结果 */
export interface WarningLocationResultVO {
  scope: string
  count: number
  items: WarningLocationItemVO[]
  cached: boolean
  degraded: boolean
  degradedSources: string[]
}

/** 仿真推演请求 */
export interface SimulationRequest {
  scenarioType: string
  pipeType?: string
  warningLevel?: string
  pipeId?: number
  params?: Record<string, unknown>
}

/** 仿真推演提交结果 */
export interface SimulationSubmitVO {
  taskId: string
  status: string
  submitTime: number
}

/** 仿真推演结论 */
export interface SimulationOutcome {
  scenarioType: string
  pipeType: string | null
  warningLevel: string | null
  impactLevel: string
  impactRadiusMeters: number
  impactAreaSquareMeters: number
  isolationSegments: string[]
  suggestions: string[]
  summary: string
}

/** 仿真推演查询结果 */
export interface SimulationResultVO {
  taskId: string
  status: string          // RUNNING / COMPLETED / FAILED
  scenarioType: string
  submitTime: number
  finishTime: number | null
  outcome: SimulationOutcome | null
  error: string | null
}
