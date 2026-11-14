<template>
  <div class="page-container">
    <!-- 搜索区域 -->
    <el-card shadow="never" class="search-card">
      <el-tabs v-model="searchMode" class="search-tabs">
        <el-tab-pane label="DID溯源查询" name="trace">
          <el-form inline class="search-form">
            <el-form-item label="管道DID">
              <el-input
                v-model="traceDid"
                placeholder="输入DID标识，如 did:lifeline:PIPE-001"
                clearable
                style="width: 380px"
                @keyup.enter="handleTrace"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="traceLoading" @click="handleTrace">
                <el-icon><Search /></el-icon>溯源查询
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="存证验证" name="verify">
          <el-form inline class="search-form">
            <el-form-item label="存证ID">
              <el-input
                v-model.number="verifyId"
                placeholder="输入存证记录ID"
                clearable
                style="width: 240px"
                @keyup.enter="handleVerify"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="verifyLoading" @click="handleVerify">
                <el-icon><Check /></el-icon>防篡改校验
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 溯源结果 -->
    <template v-if="searchMode === 'trace' && traceResult">
      <el-card shadow="never" class="trace-card">
        <template #header>
          <div class="card-header">
            <span class="card-header__title">溯源时间线</span>
            <el-tag type="info" size="small">
              DID: {{ traceResult.did }} | 共 {{ traceResult.total }} 条存证
            </el-tag>
          </div>
        </template>
        <el-timeline v-if="traceResult.timeline?.length">
          <el-timeline-item
            v-for="item in traceResult.timeline"
            :key="item.evidenceId"
            :timestamp="item.chainTime"
            placement="top"
            :type="item.chainStatus === 1 ? 'primary' : 'info'"
            :hollow="item.chainStatus !== 1"
          >
            <div class="timeline-card">
              <div class="timeline-card__header">
                <el-tag size="small" :type="evidenceTypeTag(item.evidenceType)">
                  {{ evidenceTypeLabel(item.evidenceType) }}
                </el-tag>
                <el-tag
                  size="small"
                  :type="item.chainStatus === 1 ? 'success' : 'warning'"
                  style="margin-left: 8px"
                >
                  {{ item.chainStatus === 1 ? '已上链' : '待上链' }}
                </el-tag>
              </div>
              <p class="timeline-card__content">{{ item.content || '无内容描述' }}</p>
              <div class="timeline-card__meta">
                <span class="meta-item">
                  <el-icon><Document /></el-icon>
                  关联: {{ item.relatedType }}-{{ item.relatedId }}
                </span>
                <span class="meta-item hash-text" :title="item.evidenceHash">
                  <el-icon><Key /></el-icon>
                  {{ item.evidenceHash?.substring(0, 16) }}...
                </span>
                <el-button
                  link
                  type="primary"
                  size="small"
                  @click="handleVerifyFromTimeline(item.evidenceId)"
                >验证</el-button>
              </div>
            </div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无溯源记录" />
      </el-card>
    </template>

    <!-- 验证结果 -->
    <template v-if="verifyResult">
      <el-card shadow="never" class="verify-card" :class="verifyResult.tampered ? 'verify-fail' : 'verify-pass'">
        <template #header>
          <div class="card-header">
            <span class="card-header__title">防篡改校验结果</span>
          </div>
        </template>
        <div class="verify-result">
          <div class="verify-result__icon">
            <el-icon v-if="!verifyResult.tampered" :size="48" color="#43A047"><CircleCheckFilled /></el-icon>
            <el-icon v-else :size="48" color="#E53935"><CircleCloseFilled /></el-icon>
          </div>
          <div class="verify-result__detail">
            <h3 :style="{ color: verifyResult.tampered ? '#E53935' : '#43A047' }">
              {{ verifyResult.tampered ? '验证失败 - 数据已被篡改' : '验证通过 - 数据完整' }}
            </h3>
            <el-descriptions :column="1" border size="small" class="verify-desc">
              <el-descriptions-item label="存证ID">{{ verifyResult.evidenceId }}</el-descriptions-item>
              <el-descriptions-item label="哈希算法">{{ verifyResult.hashAlgorithm }}</el-descriptions-item>
              <el-descriptions-item label="落库哈希">
                <span class="hash-text" :title="verifyResult.storedHash">{{ verifyResult.storedHash }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="重算哈希">
                <span class="hash-text" :title="verifyResult.recomputedHash" :class="{ 'hash-mismatch': verifyResult.tampered }">
                  {{ verifyResult.recomputedHash }}
                </span>
              </el-descriptions-item>
              <el-descriptions-item label="上链状态">
                <el-tag :type="verifyResult.chainStatus === 1 ? 'success' : 'warning'" size="small">
                  {{ verifyResult.chainStatus === 1 ? '已上链' : '待上链' }}
                </el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </div>
      </el-card>
    </template>

    <!-- 存证记录列表 -->
    <el-card shadow="never" class="list-card">
      <template #header>
        <div class="card-header">
          <span class="card-header__title">存证记录</span>
          <el-button type="primary" size="small" plain @click="fetchEvidenceList">刷新</el-button>
        </div>
      </template>
      <PageTable
        :columns="columns"
        :data="dataList"
        :total="total"
        :loading="tableLoading"
        :page="page"
        :size="size"
        @page-change="handlePageChange"
        @size-change="handleSizeChange"
      >
        <template #chainStatus="{ row }">
          <el-tag :type="row.confirmed ? 'success' : 'warning'" size="small">
            {{ row.confirmed ? '已确认' : '待确认' }}
          </el-tag>
        </template>
        <template #eventType="{ row }">
          <el-tag size="small">{{ row.eventType }}</el-tag>
        </template>
        <template #txHash="{ row }">
          <span class="hash-text" :title="row.txHash">{{ row.txHash?.substring(0, 12) }}...</span>
        </template>
        <template #action="{ row }">
          <el-button link type="primary" size="small" @click="handleVerifyFromTimeline(row.evidenceId)">验证</el-button>
        </template>
      </PageTable>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Check, Document, Key, CircleCheckFilled, CircleCloseFilled } from '@element-plus/icons-vue'
import PageTable from '@/components/common/PageTable.vue'
import { useTable } from '@/composables/useTable'
import { getEvidenceList, traceByDID, verifyEvidence } from '@/api/blockchain'
import type { EvidenceTraceVO, EvidenceVerifyVO } from '@/types/blockchain.d'
import type { BlockchainEvidence } from '@/types/blockchain.d'

const searchMode = ref<'trace' | 'verify'>('trace')
const traceDid = ref('')
const verifyId = ref<number | null>(null)
const traceLoading = ref(false)
const verifyLoading = ref(false)
const traceResult = ref<EvidenceTraceVO | null>(null)
const verifyResult = ref<EvidenceVerifyVO | null>(null)

// 存证类型映射
const evidenceTypeMap: Record<string, string> = {
  ORDER: '工单存证',
  CONSTRUCTION: '施工存证',
  PLAN: '预案存证',
  WARNING: '预警存证',
  install: '安装',
  inspect: '巡检',
  repair: '维修',
  replace: '更换',
}
function evidenceTypeLabel(type: string) { return evidenceTypeMap[type] || type }
function evidenceTypeTag(type: string): string {
  const map: Record<string, string> = { ORDER: '', CONSTRUCTION: 'warning', PLAN: 'success', WARNING: 'danger' }
  return map[type] || 'info'
}

// 溯源查询
async function handleTrace() {
  if (!traceDid.value.trim()) {
    ElMessage.warning('请输入管道DID')
    return
  }
  traceLoading.value = true
  verifyResult.value = null
  try {
    traceResult.value = await traceByDID(traceDid.value)
  } catch {
    traceResult.value = null
  } finally {
    traceLoading.value = false
  }
}

// 防篡改校验
async function handleVerify() {
  if (!verifyId.value) {
    ElMessage.warning('请输入存证记录ID')
    return
  }
  verifyLoading.value = true
  traceResult.value = null
  try {
    verifyResult.value = await verifyEvidence(verifyId.value)
  } catch {
    verifyResult.value = null
  } finally {
    verifyLoading.value = false
  }
}

// 从时间线跳转验证
async function handleVerifyFromTimeline(evidenceId: number) {
  verifyLoading.value = true
  searchMode.value = 'verify'
  verifyId.value = evidenceId
  try {
    verifyResult.value = await verifyEvidence(evidenceId)
  } catch {
    verifyResult.value = null
  } finally {
    verifyLoading.value = false
  }
}

// 存证列表
const page = ref(1)
const size = ref(10)
const { loading: tableLoading, dataList, total, fetch: fetchList, handlePageChange: hpChange, handleSizeChange: hsChange } = useTable<BlockchainEvidence>({
  fetchApi: getEvidenceList,
  immediate: true,
})

function fetchEvidenceList() { fetchList() }
function handlePageChange(p: number) { page.value = p; hpChange(p) }
function handleSizeChange(s: number) { size.value = s; hsChange(s) }

const columns = [
  { prop: 'evidenceId', label: '存证ID', width: 90, align: 'center' as const },
  { prop: 'eventType', label: '类型', width: 100, slot: 'eventType' },
  { prop: 'pipeId', label: '管网ID', width: 90, align: 'center' as const },
  { prop: 'operator', label: '操作人', width: 100 },
  { prop: 'txHash', label: '交易哈希', minWidth: 140, slot: 'txHash' },
  { prop: 'blockNumber', label: '区块号', width: 90, align: 'center' as const },
  { prop: 'confirmed', label: '状态', width: 90, align: 'center' as const, slot: 'chainStatus' },
  { prop: 'timestamp', label: '时间', width: 170 },
  { prop: 'action', label: '操作', width: 80, fixed: 'right' as const, slot: 'action' },
]
</script>

<style scoped lang="scss">
.page-container {
  padding: $spacing-md;
}

.search-card {
  margin-bottom: $spacing-md;

  :deep(.el-card__body) {
    padding-bottom: $spacing-sm;
  }
}

.search-form {
  padding-top: $spacing-sm;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;

  &__title {
    font-size: 15px;
    font-weight: 600;
    color: $text-primary;
  }
}

.trace-card, .verify-card, .list-card {
  margin-bottom: $spacing-md;
}

.timeline-card {
  background: $bg-color;
  border-radius: $border-radius-lg;
  padding: $spacing-md;

  &__header {
    margin-bottom: $spacing-sm;
  }

  &__content {
    font-size: 14px;
    color: $text-regular;
    line-height: 1.6;
    margin-bottom: $spacing-sm;
  }

  &__meta {
    display: flex;
    align-items: center;
    gap: $spacing-md;
    font-size: 12px;
    color: $text-secondary;

    .meta-item {
      display: flex;
      align-items: center;
      gap: 4px;
    }
  }
}

.hash-text {
  font-family: 'Courier New', monospace;
  font-size: 12px;
  color: $text-secondary;
}

.hash-mismatch {
  color: $danger-color;
  font-weight: 600;
}

.verify-result {
  display: flex;
  align-items: flex-start;
  gap: $spacing-lg;

  &__icon {
    flex-shrink: 0;
    padding-top: 4px;
  }

  &__detail {
    flex: 1;

    h3 {
      font-size: 16px;
      margin-bottom: $spacing-md;
    }
  }
}

.verify-desc {
  max-width: 600px;
}

.verify-pass {
  border-left: 4px solid $success-color;
}

.verify-fail {
  border-left: 4px solid $danger-color;
}
</style>
