<template>
  <div class="page-container">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div v-for="item in levelCards" :key="item.key" class="stat-card" :style="{ borderLeftColor: item.color }">
        <div class="stat-card__value" :style="{ color: item.color }">{{ item.count }}</div>
        <div class="stat-card__label">{{ item.label }}</div>
      </div>
    </div>

    <!-- 搜索表单 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="预警等级">
          <el-select v-model="query.level" placeholder="全部等级" clearable style="width: 140px">
            <el-option label="蓝色预警" value="blue" />
            <el-option label="黄色预警" value="yellow" />
            <el-option label="橙色预警" value="orange" />
            <el-option label="红色预警" value="red" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px">
            <el-option label="待确认" value="pending" />
            <el-option label="已确认" value="confirmed" />
            <el-option label="处置中" value="handling" />
            <el-option label="已解除" value="resolved" />
            <el-option label="误报" value="ignored" />
          </el-select>
        </el-form-item>
        <el-form-item label="管网类型">
          <el-select v-model="query.pipeType" placeholder="全部类型" clearable style="width: 140px">
            <el-option v-for="t in pipeTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
            @change="handleDateChange"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <PageTable
      :columns="columns"
      :data="dataList"
      :total="total"
      :loading="loading"
      :page="query.page!"
      :size="query.size!"
      @page-change="handlePageChange"
      @size-change="handleSizeChange"
    >
      <template #level="{ row }">
        <StatusTag :level="row.level" :label="levelLabelMap[row.level] || row.level" />
      </template>
      <template #status="{ row }">
        <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabelMap[row.status] || row.status }}</el-tag>
      </template>
      <template #threshold="{ row }">
        {{ row.metricValue }} / {{ row.thresholdValue }}
      </template>
      <template #action="{ row }">
        <el-button link type="primary" size="small" @click="handleDetail(row)">详情</el-button>
        <el-button v-if="row.status === 'pending'" link type="primary" size="small" @click="handleConfirm(row)">确认</el-button>
        <el-button v-if="row.status === 'confirmed' || row.status === 'handling'" link type="warning" size="small" @click="handleCancel(row)">核销</el-button>
      </template>
    </PageTable>

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawerVisible" title="预警详情" size="480px">
      <template v-if="currentRow">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="预警编号">{{ currentRow.warningCode }}</el-descriptions-item>
          <el-descriptions-item label="标题">{{ currentRow.title }}</el-descriptions-item>
          <el-descriptions-item label="预警等级">
            <StatusTag :level="currentRow.level" :label="levelLabelMap[currentRow.level] || currentRow.level" />
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(currentRow.status)" size="small">{{ statusLabelMap[currentRow.status] || currentRow.status }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="管网类型">{{ currentRow.pipeType }}</el-descriptions-item>
          <el-descriptions-item label="触发值 / 阈值">{{ currentRow.metricValue }} / {{ currentRow.thresholdValue }}</el-descriptions-item>
          <el-descriptions-item label="AI评分">{{ currentRow.aiScore ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="来源">{{ sourceLabelMap[currentRow.source] || currentRow.source }}</el-descriptions-item>
          <el-descriptions-item label="描述">{{ currentRow.description }}</el-descriptions-item>
          <el-descriptions-item label="触发时间">{{ currentRow.createTime }}</el-descriptions-item>
          <el-descriptions-item label="解除时间">{{ currentRow.resolveTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="处理人">{{ currentRow.handlerName || '-' }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="currentRow.reason" class="drawer-section">
          <h4>处理原因</h4>
          <p>{{ currentRow.reason }}</p>
        </div>
      </template>
    </el-drawer>

    <!-- 确认/核销对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="420px" :close-on-click-modal="false">
      <el-form label-width="80px">
        <el-form-item label="原因说明">
          <el-input v-model="dialogReason" type="textarea" :rows="3" placeholder="请输入原因说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dialogLoading" @click="handleDialogSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageTable from '@/components/common/PageTable.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { useTable } from '@/composables/useTable'
import { getWarnings, getWarningById, confirmWarning, cancelWarning, getWarningStatistics } from '@/api/warning'
import type { WarningEvent, WarningStatistics } from '@/types/warning.d'

const pipeTypes = ['供水', '排水', '燃气', '供热', '电力', '通信']

const levelLabelMap: Record<string, string> = { blue: '蓝色预警', yellow: '黄色预警', orange: '橙色预警', red: '红色预警' }
const statusLabelMap: Record<string, string> = { pending: '待确认', confirmed: '已确认', handling: '处置中', resolved: '已解除', ignored: '误报' }
const sourceLabelMap: Record<string, string> = { RULE: '规则触发', AI: 'AI识别', MANUAL: '人工上报' }
const levelColorMap: Record<string, string> = { blue: '#1E88E5', yellow: '#FDD835', orange: '#FB8C00', red: '#E53935' }

function statusTagType(status: string): string {
  const map: Record<string, string> = { pending: 'info', confirmed: 'warning', handling: '', resolved: 'success', ignored: 'danger' }
  return map[status] || 'info'
}

// 统计数据
const stats = ref<WarningStatistics | null>(null)
const levelCards = computed(() => {
  const cards = [
    { key: 'blue', label: '蓝色预警', color: '#1E88E5', count: 0 },
    { key: 'yellow', label: '黄色预警', color: '#FDD835', count: 0 },
    { key: 'orange', label: '橙色预警', color: '#FB8C00', count: 0 },
    { key: 'red', label: '红色预警', color: '#E53935', count: 0 },
  ]
  if (stats.value?.byLevel) {
    for (const item of stats.value.byLevel) {
      const card = cards.find(c => c.key === item.key)
      if (card) card.count = item.count
    }
  }
  return cards
})

// 表格
const { loading, dataList, total, query, fetch, reset, handlePageChange, handleSizeChange } = useTable<WarningEvent>({
  fetchApi: getWarnings,
  immediate: false,
})

const dateRange = ref<string[]>([])

function handleDateChange(val: string[] | null) {
  if (val && val.length === 2) {
    query.startDate = val[0]
    query.endDate = val[1]
  } else {
    delete query.startDate
    delete query.endDate
  }
}

function handleReset() {
  dateRange.value = []
  delete query.startDate
  delete query.endDate
  reset()
}

// 表格列
const columns = [
  { prop: 'warningCode', label: '预警编号', width: 150 },
  { prop: 'title', label: '标题', minWidth: 160 },
  { prop: 'pipeType', label: '管网类型', width: 100 },
  { prop: 'level', label: '预警等级', width: 110, slot: 'level' },
  { prop: 'status', label: '状态', width: 100, slot: 'status' },
  { prop: 'threshold', label: '触发值/阈值', width: 130, slot: 'threshold' },
  { prop: 'aiScore', label: 'AI评分', width: 90, align: 'center' as const },
  { prop: 'source', label: '来源', width: 100 },
  { prop: 'createTime', label: '时间', width: 170 },
  { prop: 'action', label: '操作', width: 180, fixed: 'right' as const, slot: 'action' },
]

// 详情抽屉
const drawerVisible = ref(false)
const currentRow = ref<WarningEvent | null>(null)

async function handleDetail(row: WarningEvent) {
  try {
    const detail = await getWarningById(row.warningId)
    currentRow.value = detail
  } catch {
    currentRow.value = row
  }
  drawerVisible.value = true
}

// 确认/核销对话框
const dialogVisible = ref(false)
const dialogTitle = ref('')
const dialogReason = ref('')
const dialogLoading = ref(false)
let dialogAction: 'confirm' | 'cancel' = 'confirm'
let dialogRow: WarningEvent | null = null

function handleConfirm(row: WarningEvent) {
  dialogAction = 'confirm'
  dialogRow = row
  dialogTitle.value = '确认预警'
  dialogReason.value = ''
  dialogVisible.value = true
}

function handleCancel(row: WarningEvent) {
  dialogAction = 'cancel'
  dialogRow = row
  dialogTitle.value = '核销预警'
  dialogReason.value = ''
  dialogVisible.value = true
}

async function handleDialogSubmit() {
  if (!dialogRow) return
  dialogLoading.value = true
  try {
    if (dialogAction === 'confirm') {
      await confirmWarning(dialogRow.warningId)
      ElMessage.success('预警已确认')
    } else {
      await cancelWarning(dialogRow.warningId)
      ElMessage.success('预警已核销')
    }
    dialogVisible.value = false
    fetch()
    loadStats()
  } catch {
    // error handled by interceptor
  } finally {
    dialogLoading.value = false
  }
}

async function loadStats() {
  try {
    stats.value = await getWarningStatistics()
  } catch {
    // ignore
  }
}

onMounted(() => {
  fetch()
  loadStats()
})
</script>

<style scoped lang="scss">
.page-container {
  padding: $spacing-md;
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: $spacing-md;
  margin-bottom: $spacing-md;
}

.stat-card {
  background: #fff;
  border-radius: $border-radius-lg;
  padding: $spacing-lg;
  border-left: 4px solid #ccc;
  box-shadow: $shadow-light;
  transition: transform 0.2s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: $shadow-base;
  }

  &__value {
    font-size: 32px;
    font-weight: 700;
    line-height: 1.2;
  }

  &__label {
    font-size: 14px;
    color: $text-secondary;
    margin-top: $spacing-xs;
  }
}

.search-card {
  margin-bottom: $spacing-md;

  :deep(.el-card__body) {
    padding-bottom: 0;
  }
}

.drawer-section {
  margin-top: $spacing-lg;

  h4 {
    font-size: 14px;
    color: $text-primary;
    margin-bottom: $spacing-sm;
  }

  p {
    font-size: 13px;
    color: $text-regular;
    line-height: 1.6;
  }
}
</style>
