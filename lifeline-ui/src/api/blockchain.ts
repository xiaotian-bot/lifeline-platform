import { get, post } from '@/utils/request'
import type { PageResult, PageQuery } from '@/types/api.d'
import type { BlockchainEvidence, PipeDID, EvidenceTraceVO, EvidenceVerifyVO, EvidenceVO, EvidenceRequest } from '@/types/blockchain.d'

/** 存证记录列表 */
export function getEvidenceList(params: PageQuery & Record<string, unknown>): Promise<PageResult<BlockchainEvidence>> {
  return get('/api/v1/blockchain/evidence', params as Record<string, unknown>)
}

/** 存证详情 */
export function getEvidenceById(evidenceId: number): Promise<BlockchainEvidence> {
  return get(`/api/v1/blockchain/evidence/${evidenceId}`)
}

/** 上链存证 */
export function createEvidence(data: EvidenceRequest): Promise<EvidenceVO> {
  return post('/api/v1/blockchain/evidence', data)
}

/** 查询管道DID */
export function getPipeDID(pipeId: number): Promise<PipeDID> {
  return get(`/api/v1/blockchain/did/${pipeId}`)
}

/** 按DID溯源 */
export function traceByDID(did: string): Promise<EvidenceTraceVO> {
  return get(`/api/v1/blockchain/trace/${encodeURIComponent(did)}`)
}

/** 防篡改校验 */
export function verifyEvidence(evidenceId: number): Promise<EvidenceVerifyVO> {
  return get(`/api/v1/blockchain/verify/${evidenceId}`)
}
