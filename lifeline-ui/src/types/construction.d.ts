/** 施工项目（列表/表单） */
export interface ConstructionProject {
  id: number
  projectName: string
  location: string
  contractor: string
  supervisor: string
  startDate: string
  endDate: string
  status: number            // 0待开工 1施工中 2已完工 3已停工
  statusText: string
  longitude: number
  latitude: number
  geomWkt: string
  createdAt: string
  updatedAt: string
}

/** 施工项目详情聚合 */
export interface ProjectDetail {
  project: ConstructionProject
  processes: ConstructionProcess[]
  violations: ConstructionViolation[]
  media: ConstructionMedia[]
}

/** 施工工序 */
export interface ConstructionProcess {
  id: number
  projectId: number
  processName: string
  processType: string
  status: number            // 0待质检 1质检中 2质检通过 3质检不通过
  inspector: string
  inspectTime: string
  qualityScore: number
  createdAt: string
  updatedAt: string
}

/** 施工违规记录 */
export interface ConstructionViolation {
  id: number
  projectId: number
  violationType: string
  description: string
  evidenceUrl: string
  status: number            // 0待处理 1已处理
  blockchainHash: string
  createdAt: string
  updatedAt: string
}

/** 施工影像 */
export interface ConstructionMedia {
  id: number
  projectId: number
  mediaType: string         // image / video
  fileUrl: string
  captureTime: string
  cameraId: string
  createdAt: string
}

/** AI视觉质检结果 */
export interface VisionInspectResult {
  processId: number
  processStatus: number
  processStatusText: string
  degraded: boolean
  violation: boolean
  violationType: string
  confidence: number
  evidenceUrl: string
  description: string
  violationId: number
  qualityScore: number
}

/** 影像上传结果 */
export interface MediaUploadResult {
  id: number
  projectId: number
  mediaType: string
  objectKey: string
  captureTime: string
}

/** 质检记录（旧接口兼容） */
export interface QualityInspection {
  inspectionId: number
  projectId: number
  inspectionType: string
  inspector: string
  result: string
  description: string
  inspectDate: string
  attachments: string[]
  createTime: string
}
