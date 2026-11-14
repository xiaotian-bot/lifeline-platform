/** 管网段 */
export interface PipeSegment {
  pipeId: number
  pipeCode: string
  pipeName?: string
  pipeType: string        // gas/water/drainage/heating/storm
  material: string
  diameter: number
  length: number
  depth: number
  pressureLevel?: string
  status: string          // 正常/预警/故障/废弃
  startNodeId?: number
  endNodeId?: number
  startLng: number
  startLat: number
  endLng: number
  endLat: number
  installDate?: string
  lastInspectDate?: string
  createTime?: string
  attributes?: Record<string, string>
}

/** 井盖/井室 */
export interface Manhole {
  manholeId: number
  manholeCode: string
  manholeName?: string
  manholeType: string
  diameter?: number
  depth?: number
  lng: number
  lat: number
  address?: string
  status?: string
  pipeId?: number
  createTime?: string
}

/** 监测设备 */
export interface MonitorDevice {
  deviceId: number
  deviceCode: string
  deviceName: string
  deviceType: string
  pipeType?: string
  pipeId?: number
  pipeName?: string
  manholeId?: number
  lng: number
  lat: number
  status: number        // 0=离线, 1=在线, 2=故障
  batteryLevel?: number
  lastOnlineTime?: string
  createTime?: string
}

/** GeoJSON Feature */
export interface GeoJsonFeature {
  type: 'Feature'
  geometry: {
    type: string
    coordinates: number[][] | number[][][]
  }
  properties: Record<string, unknown>
}

/** GeoJSON FeatureCollection */
export interface GeoJsonFeatureCollection {
  type: 'FeatureCollection'
  features: GeoJsonFeature[]
}

/** 一管一档响应 */
export interface PipeArchive {
  pipe: PipeSegment
  devices: MonitorDevice[]
  manholes: Manhole[]
  topology?: {
    upstreamPipes: PipeSegment[]
    downstreamPipes: PipeSegment[]
  }
}
