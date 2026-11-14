import { get, post, put, del } from '@/utils/request'
import type { PageResult, PageQuery } from '@/types/api.d'
import type { PipeSegment, Manhole, MonitorDevice, GeoJsonFeatureCollection, PipeArchive } from '@/types/pipe.d'

/** 管网列表 */
export function getPipes(params: PageQuery & Record<string, unknown>): Promise<PageResult<PipeSegment>> {
  return get('/api/v1/pipes', params as Record<string, unknown>)
}

/** 管网详情 */
export function getPipeById(pipeId: number): Promise<PipeSegment> {
  return get(`/api/v1/pipes/${pipeId}`)
}

/** 管网扩展详情 */
export function getPipeDetail(pipeId: number): Promise<Record<string, unknown>> {
  return get(`/api/v1/pipes/${pipeId}/detail`)
}

/** 管网GeoJSON */
export function getPipeGeoJson(pipeType?: string): Promise<GeoJsonFeatureCollection> {
  return get('/api/v1/pipes/geojson', pipeType ? { pipeType } : {})
}

/** 一管一档 */
export function getPipeArchive(pipeId: number): Promise<PipeArchive> {
  return get(`/api/v1/pipes/${pipeId}/archive`)
}

/** 管网挂载设备 */
export function getDevicesByPipeId(pipeId: number): Promise<MonitorDevice[]> {
  return get(`/api/v1/pipes/${pipeId}/devices`)
}

/** 创建管网 */
export function createPipe(data: Partial<PipeSegment>): Promise<number> {
  return post('/api/v1/pipes', data)
}

/** 更新管网 */
export function updatePipe(pipeId: number, data: Partial<PipeSegment>): Promise<void> {
  return put(`/api/v1/pipes/${pipeId}`, data)
}

/** 删除管网 */
export function deletePipe(pipeId: number): Promise<void> {
  return del(`/api/v1/pipes/${pipeId}`)
}

/** 井盖/井室列表 */
export function getManholes(params: PageQuery & Record<string, unknown>): Promise<PageResult<Manhole>> {
  return get('/api/v1/manholes', params as Record<string, unknown>)
}

/** 创建井盖 */
export function createManhole(data: Partial<Manhole>): Promise<number> {
  return post('/api/v1/manholes', data)
}

/** 设备列表 */
export function getDevices(params: PageQuery & Record<string, unknown>): Promise<PageResult<MonitorDevice>> {
  return get('/api/v1/devices', params as Record<string, unknown>)
}

/** 设备详情 */
export function getDeviceById(deviceId: number): Promise<MonitorDevice> {
  return get(`/api/v1/devices/${deviceId}`)
}

/** 创建设备 */
export function createDevice(data: Partial<MonitorDevice>): Promise<number> {
  return post('/api/v1/devices', data)
}
