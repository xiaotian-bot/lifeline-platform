<template>
  <div class="plan-instance-page">
    <!-- 统计卡片 -->
    <div class="stats-bar">
      <div
        v-for="item in statsCards"
        :key="item.key"
        class="stat-card"
        :style="{ borderTopColor: item.color }"
      >
        <div class="stat-count">{{ statusCounts[item.key] ?? 0 }}</div>
        <div class="stat-label">{{ item.label }}</div>
      </div>
    </div>

    <!-- 搜索区 -->
    <div class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
            <el-option label="待执行" :value="0" />
            <el-option label="执行中" :value="1" />
            <el-option label="已完成" :value="2" />
            <el-option label="已归档" :value="3" />
            <el-option label="已取消" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源">
          <el-select v-model="query.source" placeholder="全部来源" clearable style="width: 120px">
            <el-option label="AI生成" value="AI" />
            <el-option label="模板实例化" value="TEMPLATE" />
          </el-select>
        </el-form-item>
        <el-form-item label="联动级别">
          <el-select v-model="query.escalationLevel" placeholder="全部级别" clearable style="width: 130px">
            <el-option label="企业运维" :value="0" />
            <el-option label="主管部门" :value="1" />
            <el-option label="应急联动" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetch">查询</el-button>
          <el-button @click="reset">重置</el-button>
          <el-button type="warning" plain @click="openGenerateDialog">AI 生成预案</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 表格 -->
    <div class="table-wrapper">
      <PageTable
        :columns="columns"
        :data="dataList"
        :total="total"
        :loading="loading"
        :page="query.page as number"
        :size="query.size as number"
        @page-change="handlePageChange"
        @size-change="handleSizeChange"
      >
        <template #source="{ row }">
          <el-tag :type="row.source === 'AI' ? '' : 'info'" size="small">
            {{ row.source === 'AI' ? 'AI生成' : '模板' }}
          </el-tag>
        </template>
        <template #status="{ row }">
          <el-tag :type="instanceStatusType(row.status)" size="small">
            {{ instanceStatusLabel(row.status) }}
          </el-tag>
        </template>
        <template #escalationLevel="{ row }">
          <el-tag :type="escalationType(row.escalationLevel)" size="small">
            {{ escalationLabel(row.escalationLevel) }}
          </el-tag>
        </template>
        <template #progress="{ row }">
          <span v-if="row.taskTotal">{{ row.taskCompleted ?? 0 }} / {{ row.taskTotal }}</span>
          <span v-else>-</span>
        </template>
        <template #action="{ row }">
          <el-button link type="primary" size="small" @click="handleViewDetail(row)">详情</el-button>
          <el-button
            v-if="row.status === 0"
            link type="success" size="small"
            @click="handleExecute(row)"
          >启动</el-button>
          <el-button
            v-if="row.status === 1 && row.escalationLevel < 2"
            link type="warning" size="small"
            @click="handleEscalate(row)"
          >上报</el-button>
          <el-button
            v-if="row.status === 2"
            link type="info" size="small"
            @click="handleArchive(row)"
          >归档</el-button>
          <el-button
            v-if="row.status === 0 || row.status === 1"
            link type="danger" size="small"
            @click="handleCancel(row)"
          >取消</el-button>
          <el-button
            v-if="row.status === 0 || row.status === 1"
            link type="primary" size="small"
            @click="goToTasks(row)"
          >任务</el-button>
        </template>
      </PageTable>
    </div>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailDrawerVisible" title="预案详情" size="580px">
      <template v-if="planDetail">
        <!-- 实例基本信息 -->
        <el-descriptions :column="2" border>
          <el-descriptions-item label="实例编号">{{ planDetail.instance.instanceCode }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="instanceStatusType(planDetail.instance.status)" size="small">
              {{ instanceStatusLabel(planDetail.instance.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="预案名称" :span="2">{{ planDetail.instance.planName }}</el-descriptions-item>
          <el-descriptions-item label="来源">
            <el-tag :type="planDetail.instance.source === 'AI' ? '' : 'info'" size="small">
              {{ planDetail.instance.source === 'AI' ? 'AI生成' : '模板实例化' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="联动级别">
            <el-tag :type="escalationType(planDetail.instance.escalationLevel)" size="small">
              {{ escalationLabel(planDetail.instance.escalationLevel) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="关联预警ID">{{ planDetail.instance.warningEventId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="场景类型">{{ scenarioLabelMap[planDetail.instance.scenarioType] || '-' }}</el-descriptions-item>
          <el-descriptions-item label="启动时间">{{ planDetail.instance.startTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">{{ planDetail.instance.endTime || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="planDetail.instance.suggestion" label="处置建议" :span="2">
            {{ planDetail.instance.suggestion }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 任务进度 -->
        <div class="drawer-section">
          <h4>任务清单（{{ completedTaskCount }}/{{ planDetail.tasks.length }}）</h4>
          <el-progress
            :percentage="taskProgressPercent"
            :status="taskProgressPercent === 100 ? 'success' : undefined"
            style="margin-bottom: 12px"
          />
          <el-table :data="planDetail.tasks" size="small" border v-if="planDetail.tasks.length">
            <el-table-column prop="taskName" label="任务名称" min-width="120" />
            <el-table-column prop="taskType" label="类型" width="80" align="center" />
            <el-table-column prop="assignee" label="负责人" width="90" />
            <el-table-column label="优先级" width="70" align="center">
              <template #default="{ row }">
                <el-tag :type="priorityType(row.priority)" size="small">{{ priorityLabel(row.priority) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="taskStatusType(row.status)" size="small">{{ row.statusText }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="暂无任务" :image-size="48" />
        </div>

        <!-- 资源清单 -->
        <div class="drawer-section" v-if="planDetail.resources?.length">
          <h4>资源清单</h4>
          <el-table :data="planDetail.resources" size="small" border>
            <el-table-column prop="resourceName" label="资源名称" min-width="120" />
            <el-table-column prop="resourceType" label="类型" width="80" align="center" />
            <el-table-column prop="quantity" label="数量" width="70" align="center" />
            <el-table-column prop="location" label="位置" width="100" />
            <el-table-column label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="resourceStatusType(row.status)" size="small">
                  {{ resourceStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 执行日志 -->
        <div class="drawer-section" v-if="planDetail.logs?.length">
          <h4>执行日志</h4>
          <el-timeline>
            <el-timeline-item
              v-for="log in planDetail.logs"
              :key="log.id"
              :timestamp="log.logTime"
              placement="top"
            >
              <span class="log-action">{{ log.action }}</span>
              <span class="log-operator">{{ log.operator }}</span>
              <p v-if="log.content" class="log-content">{{ log.content }}</p>
            </el-timeline-item>
          </el-timeline>
        </div>
      </template>
      <el-skeleton v-else :rows="6" animated />
    </el-drawer>

    <!-- AI 生成预案对话框 -->
    <el-dialog v-model="generateDialogVisible" title="AI 智能生成预案" width="520px" :close-on-click-modal="false">
      <el-form :model="generateForm" :rules="generateRules" ref="generateFormRef" label-width="90px">
        <el-form-item label="预警事件ID" prop="warningEventId">
          <el-input-number v-model="generateForm.warningEventId" :min="1" placeholder="请输入" style="width: 100%" />
        </el-form-item>
        <el-form-item label="场景类型">
          <el-select v-model="generateForm.scenarioType" placeholder="自动识别" clearable style="width: 100%">
            <el-option v-for="item in scenarioOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="事发位置">
          <el-input v-model="generateForm.locationDesc" placeholder="选填，如：安塞区真武街与延河路交叉口" />
        </el-form-item>
        <el-form-item label="管网类型">
          <el-select v-model="generateForm.pipeType" placeholder="选填" clearable style="width: 100%">
            <el-option label="燃气" value="gas" />
            <el-option label="供水" value="water" />
            <el-option label="排水" value="drainage" />
            <el-option label="供热" value="heating" />
          </el-select>
        </el-form-item>
        <el-form-item label="预警标题">
          <el-input v-model="generateForm.title" placeholder="选填" />
        </el-form-item>
        <el-form-item label="预警描述">
          <el-input v-model="generateForm.description" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="generateDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="generateLoading" @click="handleGenerate">
          <el-icon style="margin-right: 4px"><MagicStick /></el-icon>
          生成预案
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { MagicStick } from '@element-plus/icons-vue'
import PageTable from '@/components/common/PageTable.vue'
import { useTable } from '@/composables/useTable'
import {
  fetchPlanInstances,
  fetchPlanDetail,
  executePlan,
  escalatePlan,
  cancelPlan,
  archivePlan,
  generatePlan,
} from '@/api/plan'
import type { PlanInstanceVO, PlanDetailVO, PlanGenerateRequest } from '@/types/plan.d'

const router = useRouter()

/* ==================== 映射表 ==================== */
const scenarioOptions = [
  { value: 'GAS_LEAK', label: '燃气泄漏' },
  { value: 'WATER_BURST', label: '供水爆管' },
  { value: 'HEATING_LEAK', label: '供热泄漏' },
  { value: 'DRAINAGE_OVERFLOW', label: '排水溢流' },
  { value: 'ROAD_COLLAPSE', label: '道路塌陷' },
  { value: 'THIRD_PARTY_DAMAGE', label: '第三方破坏' },
  { value: 'CORROSION_FAILURE', label: '腐蚀失效' },
  { value: 'COMPREHENSIVE', label: '综合场景' },
]

const scenarioLabelMap: Record<string, string> = {
  GAS_LEAK: '燃气泄漏',
  WATER_BURST: '供水爆管',
  HEATING_LEAK: '供热泄漏',
  DRAINAGE_OVERFLOW: '排水溢流',
  ROAD_COLLAPSE: '道路塌陷',
  THIRD_PARTY_DAMAGE: '第三方破坏',
  CORROSION_FAILURE: '腐蚀失效',
  COMPREHENSIVE: '综合场景',
}

function instanceStatusLabel(status: number): string {
  const map: Record<number, string> = { 0: '待执行', 1: '执行中', 2: '已完成', 3: '已归档', 4: '已取消' }
  return map[status] ?? String(status)
}

function instanceStatusType(status: number): string {
  const map: Record<number, string> = { 0: 'info', 1: '', 2: 'success', 3: 'info', 4: 'danger' }
  return map[status] ?? 'info'
}

function escalationLabel(level: number): string {
  const map: Record<number, string> = { 0: '企业运维', 1: '主管部门', 2: '应急联动' }
  return map[level] ?? String(level)
}

function escalationType(level: number): string {
  const map: Record<number, string> = { 0: 'info', 1: 'warning', 2: 'danger' }
  return map[level] ?? 'info'
}

function priorityLabel(p: number): string {
  const map: Record<number, string> = { 1: '高', 2: '中', 3: '低' }
  return map[p] ?? String(p)
}

function priorityType(p: number): string {
  const map: Record<number, string> = { 1: 'danger', 2: 'warning', 3: 'info' }
  return map[p] ?? 'info'
}

function taskStatusType(status: number): string {
  const map: Record<number, string> = { 0: 'info', 1: '', 2: 'success', 3: 'danger' }
  return map[status] ?? 'info'
}

function resourceStatusLabel(status: number): string {
  const map: Record<number, string> = { 0: '待调配', 1: '已调配', 2: '已到位' }
  return map[status] ?? String(status)
}

function resourceStatusType(status: number): string {
  const map: Record<number, string> = { 0: 'info', 1: 'warning', 2: 'success' }
  return map[status] ?? 'info'
}

/* ==================== 统计 ==================== */
const statusCounts = ref<Record<string, number>>({
  pending: 0,
  executing: 0,
  completed: 0,
  archived: 0,
})

const statsCards = [
  { key: 'pending', label: '待执行', color: '#909399' },
  { key: 'executing', label: '执行中', color: '#1E88E5' },
  { key: 'completed', label: '已完成', color: '#43A047' },
  { key: 'archived', label: '已归档', color: '#606266' },
]

/* ==================== 表格 ==================== */
const {
  loading, dataList, total, query,
  fetch, reset, handlePageChange, handleSizeChange,
} = useTable<PlanInstanceVO>({
  fetchApi: (params) => fetchPlanInstances(params),
  defaultQuery: {},
  immediate: true,
})

const columns = [
  { prop: 'instanceCode', label: '实例编号', width: 140, fixed: 'left' as const },
  { prop: 'planName', label: '预案名称', minWidth: 160 },
  { prop: 'source', label: '来源', width: 90, align: 'center' as const, slot: 'source' },
  { prop: 'status', label: '状态', width: 90, align: 'center' as const, slot: 'status' },
  { prop: 'escalationLevel', label: '联动级别', width: 100, align: 'center' as const, slot: 'escalationLevel' },
  { prop: 'progress', label: '任务进度', width: 100, align: 'center' as const, slot: 'progress' },
  { prop: 'warningEventId', label: '关联预警', width: 100, align: 'center' as const },
  { prop: 'createdAt', label: '创建时间', width: 170 },
  { prop: 'action', label: '操作', width: 220, fixed: 'right' as const, slot: 'action' },
]

/* ==================== 详情抽屉 ==================== */
const detailDrawerVisible = ref(false)
const planDetail = ref<PlanDetailVO | null>(null)

const completedTaskCount = computed(() => {
  if (!planDetail.value?.tasks) return 0
  return planDetail.value.tasks.filter(t => t.status === 2).length
})

const taskProgressPercent = computed(() => {
  if (!planDetail.value?.tasks?.length) return 0
  return Math.round((completedTaskCount.value / planDetail.value.tasks.length) * 100)
})

async function handleViewDetail(row: PlanInstanceVO) {
  detailDrawerVisible.value = true
  planDetail.value = null
  try {
    planDetail.value = await fetchPlanDetail(row.id)
  } catch {
    // handled by interceptor
  }
}

/* ==================== 状态操作 ==================== */
async function handleExecute(row: PlanInstanceVO) {
  try {
    await ElMessageBox.confirm(`确认启动预案「${row.planName}」？`, '启动确认', { type: 'info' })
    await executePlan(row.id)
    ElMessage.success('预案已启动')
    fetch()
  } catch {
    // cancel or error
  }
}

async function handleEscalate(row: PlanInstanceVO) {
  try {
    await ElMessageBox.confirm(
      `确认将预案「${row.planName}」上报至「${escalationLabel(row.escalationLevel + 1)}」级别？`,
      '三级联动上报',
      { type: 'warning' }
    )
    await escalatePlan(row.id)
    ElMessage.success('上报成功')
    fetch()
  } catch {
    // cancel or error
  }
}

async function handleArchive(row: PlanInstanceVO) {
  try {
    await ElMessageBox.confirm(`确认归档预案「${row.planName}」？`, '归档确认', { type: 'info' })
    await archivePlan(row.id)
    ElMessage.success('预案已归档')
    fetch()
  } catch {
    // cancel or error
  }
}

async function handleCancel(row: PlanInstanceVO) {
  try {
    await ElMessageBox.confirm(`确认取消预案「${row.planName}」？取消后不可恢复。`, '取消确认', { type: 'warning' })
    await cancelPlan(row.id)
    ElMessage.success('预案已取消')
    fetch()
  } catch {
    // cancel or error
  }
}

function goToTasks(row: PlanInstanceVO) {
  router.push(`/plan/instance/${row.id}/tasks`)
}

/* ==================== AI 生成 ==================== */
const generateDialogVisible = ref(false)
const generateLoading = ref(false)
const generateFormRef = ref<FormInstance>()

const generateForm = reactive<PlanGenerateRequest>({
  warningEventId: 0,
  scenarioType: undefined,
  locationDesc: undefined,
  pipeType: undefined,
  title: undefined,
  description: undefined,
})

const generateRules: FormRules = {
  warningEventId: [{ required: true, message: '请输入预警事件ID', trigger: 'blur' }],
}

function openGenerateDialog() {
  generateForm.warningEventId = 0
  generateForm.scenarioType = undefined
  generateForm.locationDesc = undefined
  generateForm.pipeType = undefined
  generateForm.title = undefined
  generateForm.description = undefined
  generateDialogVisible.value = true
}

async function handleGenerate() {
  if (!generateFormRef.value) return
  await generateFormRef.value.validate()
  generateLoading.value = true
  try {
    const data: PlanGenerateRequest = { warningEventId: generateForm.warningEventId }
    if (generateForm.scenarioType) data.scenarioType = generateForm.scenarioType
    if (generateForm.locationDesc) data.locationDesc = generateForm.locationDesc
    if (generateForm.pipeType) data.pipeType = generateForm.pipeType
    if (generateForm.title) data.title = generateForm.title
    if (generateForm.description) data.description = generateForm.description
    await generatePlan(data)
    ElMessage.success('预案生成成功')
    generateDialogVisible.value = false
    fetch()
  } catch {
    // handled by interceptor
  } finally {
    generateLoading.value = false
  }
}
</script>

<style scoped lang="scss">
.plan-instance-page {
  padding: $spacing-md;
}

.stats-bar {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: $spacing-md;
  margin-bottom: $spacing-md;
}

.stat-card {
  background: #fff;
  border-radius: $border-radius-lg;
  border-top: 3px solid #909399;
  padding: $spacing-lg $spacing-md;
  text-align: center;
  box-shadow: $shadow-light;
  transition: transform 0.2s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: $shadow-base;
  }

  .stat-count {
    font-size: 28px;
    font-weight: 700;
    color: $text-primary;
    line-height: 1.2;
  }

  .stat-label {
    font-size: 13px;
    color: $text-secondary;
    margin-top: $spacing-xs;
  }
}

.search-bar {
  margin-bottom: $spacing-md;
}

.table-wrapper {
  width: 100%;
}

.drawer-section {
  margin-top: $spacing-lg;

  h4 {
    font-size: 14px;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: $spacing-sm;
    padding-left: $spacing-xs;
    border-left: 3px solid $primary-color;
  }
}

.log-action {
  font-weight: 600;
  margin-right: $spacing-sm;
}

.log-operator {
  color: $text-secondary;
  font-size: 12px;
}

.log-content {
  color: $text-regular;
  font-size: 13px;
  margin-top: $spacing-xs;
}
</style>
