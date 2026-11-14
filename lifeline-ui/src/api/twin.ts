import { get, post } from '@/utils/request'
import type { TwinScene, TwinDataPoint, ModelDataVO, RealtimeMappingRequest, RealtimeMappingVO, WarningLocationResultVO, SimulationRequest, SimulationSubmitVO, SimulationResultVO } from '@/types/twin.d'

/** 获取孪生场景列表 */
export function getTwinScenes(): Promise<TwinScene[]> {
  return get('/api/v1/twin/scenes')
}

/** 获取场景详情 */
export function getTwinSceneById(sceneId: number): Promise<TwinScene> {
  return get(`/api/v1/twin/scenes/${sceneId}`)
}

/** 获取图层数据 */
export function getTwinLayerData(sceneId: number, layerType: string): Promise<TwinDataPoint[]> {
  return get(`/api/v1/twin/scenes/${sceneId}/layers/${layerType}`)
}

/** 模型数据聚合 */
export function getTwinModelData(params?: { pipeType?: string; areaId?: number }): Promise<ModelDataVO> {
  return get('/api/v1/twin/model-data', params as Record<string, unknown>)
}

/** 实时映射 */
export function getRealtimeMapping(data: RealtimeMappingRequest): Promise<RealtimeMappingVO> {
  return post('/api/v1/twin/realtime-mapping', data)
}

/** 预警定位 */
export function getWarningLocation(params: { warningId?: number; pipeId?: number; areaId?: number }): Promise<WarningLocationResultVO> {
  return get('/api/v1/twin/warning-location', params as Record<string, unknown>)
}

/** 提交仿真推演 */
export function submitSimulation(data: SimulationRequest): Promise<SimulationSubmitVO> {
  return post('/api/v1/twin/simulation', data)
}

/** 轮询仿真推演结果 */
export function querySimulation(taskId: string): Promise<SimulationResultVO> {
  return get(`/api/v1/twin/simulation/${taskId}`)
}
