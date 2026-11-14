/** 运维工单 */
export interface OperationOrder {
  orderId: number
  orderCode: string
  orderType: string       // repair/inspect/maintain/emergency
  priority: string        // low/medium/high/urgent
  title: string
  description: string
  source: string          // WARNING/PLAN/MANUAL
  pipeId: number
  deviceId: number
  warningId: number
  assigneeId: number
  assigneeName: string
  dispatcherId: number
  dispatcherName: string
  creatorId: number
  creatorName: string
  status: string          // pending/assigned/in-progress/completed/closed/rejected
  createTime: string
  assignTime: string
  completeTime: string
  closeTime: string
  result: string
  /** 关联预警信息 */
  warningEvent?: {
    warningId: number
    warningCode: string
    level: string
    description: string
  }
  /** 关联管网/设备信息 */
  pipeInfo?: {
    pipeId: number
    pipeCode: string
    pipeType: string
  }
  deviceInfo?: {
    deviceId: number
    deviceCode: string
    deviceType: string
  }
  /** 状态流转记录 */
  timeline?: Array<{
    status: string
    operator: string
    operateTime: string
    remark: string
  }>
}

/** 工单统计 */
export interface OrderStatistics {
  pending: number
  assigned: number
  inProgress: number
  completed: number
  closed: number
  total: number
}

/** 巡检计划 */
export interface InspectionPlan {
  planId: number
  planName: string
  planType: string        // ROUTINE/GAS/DRAINAGE
  areaId: number
  areaName: string
  frequency: string       // daily/weekly/monthly
  assigneeId: number
  assigneeName: string
  description: string
  lastExecTime: string
  nextExecTime: string
  status: string          // active/paused/completed
  createTime: string
}

/** 运维人员 */
export interface OperationStaff {
  staffId: number
  staffCode: string
  name: string
  skillType: string       // gas/drainage/power/comm
  phone: string
  status: string          // active/busy/offline
  createTime: string
}

/** 运维物资 */
export interface OperationMaterial {
  materialId: number
  materialName: string
  materialType: string    // tool/pipe/part/safety
  stock: number
  unit: string
  warehouse: string
  minStock: number
  createTime: string
}
