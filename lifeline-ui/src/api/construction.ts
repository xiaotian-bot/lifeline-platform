import { get, post, put, del } from '@/utils/request'
import type { AxiosRequestConfig } from 'axios'
import type { PageResult, PageQuery } from '@/types/api.d'
import type {
  ConstructionProject,
  ProjectDetail,
  ConstructionProcess,
  ConstructionViolation,
  ConstructionMedia,
  VisionInspectResult,
  MediaUploadResult,
} from '@/types/construction.d'

/* ==================== 施工项目 ==================== */

/** 施工项目分页列表 */
export function getConstructionProjects(params: PageQuery & Record<string, unknown>): Promise<PageResult<ConstructionProject>> {
  return get('/api/v1/constructions/projects', params as Record<string, unknown>)
}

/** 施工项目详情（聚合） */
export function getConstructionDetail(projectId: number): Promise<ProjectDetail> {
  return get(`/api/v1/constructions/projects/${projectId}`)
}

/** 创建施工项目 */
export function createConstructionProject(data: Record<string, unknown>): Promise<ConstructionProject> {
  return post('/api/v1/constructions/projects', data)
}

/** 更新施工项目 */
export function updateConstructionProject(projectId: number, data: Record<string, unknown>): Promise<ConstructionProject> {
  return put(`/api/v1/constructions/projects/${projectId}`, data)
}

/** 删除施工项目 */
export function deleteConstructionProject(projectId: number): Promise<void> {
  return del(`/api/v1/constructions/projects/${projectId}`)
}

/** 推进项目状态 */
export function advanceProjectStatus(projectId: number, target: number): Promise<ConstructionProject> {
  return post(`/api/v1/constructions/projects/${projectId}/status`, null, { params: { target } } as AxiosRequestConfig)
}

/* ==================== 工序与质检 ==================== */

/** 工序列表 */
export function getProcesses(projectId: number): Promise<ConstructionProcess[]> {
  return get(`/api/v1/constructions/projects/${projectId}/processes`)
}

/** 创建工序 */
export function createProcess(projectId: number, data: { processName: string; processType: string }): Promise<ConstructionProcess> {
  return post(`/api/v1/constructions/projects/${projectId}/processes`, data)
}

/** 提交人工质检记录 */
export function inspectProcess(processId: number, data: { inspector: string; qualityScore: number }): Promise<ConstructionProcess> {
  return post(`/api/v1/constructions/processes/${processId}/inspect`, data)
}

/** AI视觉质检 */
export function visionInspect(processId: number, data: { mediaUrl: string; mediaType?: string; cameraId?: string }): Promise<VisionInspectResult> {
  return post(`/api/v1/constructions/processes/${processId}/vision-inspect`, data)
}

/* ==================== 违规 ==================== */

/** 违规记录列表 */
export function getViolations(projectId: number): Promise<ConstructionViolation[]> {
  return get(`/api/v1/constructions/projects/${projectId}/violations`)
}

/** 上报违规 */
export function reportViolation(projectId: number, data: { violationType: string; description: string; evidenceUrl?: string }): Promise<ConstructionViolation> {
  return post(`/api/v1/constructions/projects/${projectId}/violations`, data)
}

/** 处理违规 */
export function handleViolation(violationId: number): Promise<ConstructionViolation> {
  return put(`/api/v1/constructions/violations/${violationId}/handle`)
}

/* ==================== 影像 ==================== */

/** 影像列表 */
export function getMediaList(projectId: number): Promise<ConstructionMedia[]> {
  return get(`/api/v1/constructions/projects/${projectId}/media`)
}

/** 上传影像（multipart/form-data） */
export function uploadMedia(projectId: number, file: File, cameraId?: string): Promise<MediaUploadResult> {
  const formData = new FormData()
  formData.append('file', file)
  if (cameraId) formData.append('cameraId', cameraId)
  return post(`/api/v1/constructions/projects/${projectId}/media`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  } as AxiosRequestConfig)
}
