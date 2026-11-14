/** 区块链存证记录 */
export interface BlockchainEvidence {
  evidenceId: number
  txHash: string
  blockNumber: number
  pipeId: number
  eventType: string       // install/inspect/repair/replace
  operator: string
  evidenceData: string
  timestamp: string
  confirmed: boolean
}

/** DID身份 */
export interface PipeDID {
  did: string
  pipeId: number
  document: string
  created: string
  updated: string
}

/** 存证溯源时间线条目 */
export interface EvidenceTimelineItem {
  evidenceId: number
  evidenceHash: string
  evidenceType: string
  relatedId: number
  relatedType: string
  content: string
  chainStatus: number     // 0 待上链 / 1 已上链
  chainTime: string
}

/** 存证溯源结果 */
export interface EvidenceTraceVO {
  did: string
  pipeId: number
  pipeCode: string
  total: number
  timeline: EvidenceTimelineItem[]
}

/** 存证防篡改校验结果 */
export interface EvidenceVerifyVO {
  evidenceId: number
  tampered: boolean
  storedHash: string
  recomputedHash: string
  hashAlgorithm: string
  chainStatus: number
}

/** 存证上链结果 */
export interface EvidenceVO {
  evidenceId: number
  evidenceHash: string
  chainStatus: number
  chainTime: string
  blockNo: number
  txHash: string
  hashAlgorithm: string
  adapter: string
}

/** 存证请求 */
export interface EvidenceRequest {
  evidenceType: string
  relatedId: number
  relatedType: string
  content?: string
}
