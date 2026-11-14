<template>
  <div class="order-page">
    <!-- 统计卡片 -->
    <div class="stats-bar">
      <div
        v-for="item in statsCards"
        :key="item.key"
        class="stat-card"
        :style="{ borderTopColor: item.color }"
      >
        <div class="stat-count">{{ stats[item.key] ?? 0 }}</div>
        <div class="stat-label">{{ item.label }}</div>
      </div>
    </div>

    <!-- 搜索区 -->
    <div class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="工单类型">
          <el-select v-model="query.orderType" placeholder="全部" clearable style="width: 130px">
            <el-option label="维修" value="repair" />
            <el-option label="巡检" value="inspect" />
            <el-option label="保养" value="maintain" />
            <el-option label="应急" value="emergency" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="待派单" value="pending" />
            <el-option label="待接单" value="assigned" />
            <el-option label="处理中" value="in-progress" />
            <el-option label="已完成" value="completed" />
            <el-option label="已关闭" value="closed" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="query.priority" placeholder="全部" clearable style="width: 110px">
            <el-option label="高" value="high" />
            <el-option label="中" value="medium" />
            <el-option label="低" value="low" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源">
          <el-select v-model="query.source" placeholder="全部" clearable style="width: 120px">
            <el-option label="预警事件" value="WARNING" />
            <el-option label="巡检计划" value="PLAN" />
            <el-option label="手动创建" value="MANUAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 240px"
            @change="handleDateChange"
          />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="工单编号/标题" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetch">查询</el-button>
          <el-button @click="reset">重置</el-button>
          <el-button type="primary" plain @click="openCreateDialog">新建工单</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 表格 -->
    <div class="page-container" style="padding-top: 0">
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
        <template #priority="{ row }">
          <span
            class="priority-tag"
            :style="{ backgroundColor: priorityColor(row.priority), color: '#fff' }"
          >{{ priorityLabel(row.priority) }}</span>
        </template>
        <template #status="{ row }">
          <StatusTag :status="row.status" :label="statusLabel(row.status)" />
        </template>
        <template #action="{ row }">
          <el-button link type="primary" size="small" @click="showDetail(row)">详情</el-button>
          <el-button
            v-if="row.status === 'pending'"
            link type="primary" size="small"
            @click="openAssignDialog(row)"
          >派单</el-button>
          <el-button
            v-if="row.status === 'assigned'"
            link type="success" size="small"
            @click="handleAccept(row)"
          >接单</el-button>
          <el-button
            v-if="row.status === 'in-progress'"
            link type="warning" size="small"
            @click="openCompleteDialog(row)"
          >完工</el-button>
          <el-button
            v-if="['pending', 'assigned', 'in-progress'].includes(row.status)"
            link type="danger" size="small"
            @click="handleClose(row)"
          >关闭</el-button>
        </template>
      </PageTable>
    </div>

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawerVisible" title="工单详情" size="520px">
      <template v-if="currentOrder">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="工单编号">{{ currentOrder.orderCode }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <StatusTag :status="currentOrder.status" :label="statusLabel(currentOrder.status)" />
          </el-descriptions-item>
          <el-descriptions-item label="标题" :span="2">{{ currentOrder.title }}</el-descriptions-item>
          <el-descriptions-item label="工单类型">{{ orderTypeLabel(currentOrder.orderType) }}</el-descriptions-item>
          <el-descriptions-item label="来源">{{ sourceLabel(currentOrder.source) }}</el-descriptions-item>
          <el-descriptions-item label="优先级">
            <span :style="{ color: priorityColor(currentOrder.priority) }">{{ priorityLabel(currentOrder.priority) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentOrder.createTime }}</el-descriptions-item>
          <el-descriptions-item label="派单人">{{ currentOrder.dispatcherName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="处理人">{{ currentOrder.assigneeName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ currentOrder.description || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="currentOrder.result" label="处置结果" :span="2">{{ currentOrder.result }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ currentOrder.completeTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="关闭时间">{{ currentOrder.closeTime || '-' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 关联预警 -->
        <div v-if="currentOrder.warningEvent" class="drawer-section">
          <h4>关联预警事件</h4>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="预警编号">{{ currentOrder.warningEvent.warningCode }}</el-descriptions-item>
            <el-descriptions-item label="预警等级">{{ currentOrder.warningEvent.level }}</el-descriptions-item>
            <el-descriptions-item label="描述" :span="2">{{ currentOrder.warningEvent.description }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 关联管网/设备 -->
        <div v-if="currentOrder.pipeInfo || currentOrder.deviceInfo" class="drawer-section">
          <h4>关联管网/设备</h4>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item v-if="currentOrder.pipeInfo" label="管网编号">{{ currentOrder.pipeInfo.pipeCode }}</el-descriptions-item>
            <el-descriptions-item v-if="currentOrder.pipeInfo" label="管网类型">{{ currentOrder.pipeInfo.pipeType }}</el-descriptions-item>
            <el-descriptions-item v-if="currentOrder.deviceInfo" label="设备编号">{{ currentOrder.deviceInfo.deviceCode }}</el-descriptions-item>
            <el-descriptions-item v-if="currentOrder.deviceInfo" label="设备类型">{{ currentOrder.deviceInfo.deviceType }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 状态时间线 -->
        <div v-if="currentOrder.timeline?.length" class="drawer-section">
          <h4>状态流转</h4>
          <el-timeline>
            <el-timeline-item
              v-for="(item, idx) in currentOrder.timeline"
              :key="idx"
              :timestamp="item.operateTime"
              placement="top"
            >
              <span class="timeline-status">{{ statusLabel(item.status) }}</span>
              <span class="timeline-operator">操作人: {{ item.operator }}</span>
              <p v-if="item.remark" class="timeline-remark">{{ item.remark }}</p>
            </el-timeline-item>
          </el-timeline>
        </div>
      </template>
    </el-drawer>

    <!-- 派单对话框 -->
    <el-dialog v-model="assignDialogVisible" title="工单派单" width="420px" :close-on-click-modal="false">
      <el-form :model="assignForm" label-width="80px">
        <el-form-item label="工单">{{ assignForm.orderCode }}</el-form-item>
        <el-form-item label="处理人" required>
          <el-select v-model="assignForm.assigneeId" placeholder="请选择处理人" style="width: 100%">
            <el-option
              v-for="s in staffOptions"
              :key="s.staffId"
              :label="`${s.name} (${s.skillType})`"
              :value="s.staffId"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleAssign">确认派单</el-button>
      </template>
    </el-dialog>

    <!-- 完工对话框 -->
    <el-dialog v-model="completeDialogVisible" title="完工登记" width="480px" :close-on-click-modal="false">
      <el-form :model="completeForm" label-width="80px">
        <el-form-item label="工单">{{ completeForm.orderCode }}</el-form-item>
        <el-form-item label="处置结果" required>
          <el-input
            v-model="completeForm.result"
            type="textarea"
            :rows="4"
            placeholder="请填写处置结果"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleComplete">确认完工</el-button>
      </template>
    </el-dialog>

    <!-- 新建工单对话框 -->
    <el-dialog v-model="createDialogVisible" title="新建工单" width="540px" :close-on-click-modal="false">
      <el-form :model="createForm" :rules="createRules" ref="createFormRef" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="createForm.title" placeholder="请输入工单标题" />
        </el-form-item>
        <el-form-item label="工单类型" prop="orderType">
          <el-select v-model="createForm.orderType" placeholder="请选择" style="width: 100%">
            <el-option label="维修" value="repair" />
            <el-option label="巡检" value="inspect" />
            <el-option label="保养" value="maintain" />
            <el-option label="应急" value="emergency" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-select v-model="createForm.priority" placeholder="请选择" style="width: 100%">
            <el-option label="高" value="high" />
            <el-option label="中" value="medium" />
            <el-option label="低" value="low" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" :rows="3" placeholder="请输入工单描述" />
        </el-form-item>
        <el-form-item label="关联管网">
          <el-input v-model.number="createForm.pipeId" placeholder="管网ID（选填）" />
        </el-form-item>
        <el-form-item label="关联设备">
          <el-input v-model.number="createForm.deviceId" placeholder="设备ID（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import PageTable from '@/components/common/PageTable.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { useTable } from '@/composables/useTable'
import {
  getOrders,
  getOrderStatistics,
  getOrderById,
  createOrder,
  updateOrder,
  acceptOrder,
  completeOrder,
  closeOrder,
  getOperationStaff,
} from '@/api/operation'
import type { OperationOrder, OrderStatistics, OperationStaff } from '@/types/operation.d'

/* ==================== 映射表 ==================== */
const statusMap: Record<string, string> = {
  pending: '待派单',
  assigned: '待接单',
  'in-progress': '处理中',
  completed: '已完成',
  closed: '已关闭',
  rejected: '已驳回',
}
const orderTypeMap: Record<string, string> = {
  repair: '维修',
  inspect: '巡检',
  maintain: '保养',
  emergency: '应急',
}
const sourceMap: Record<string, string> = {
  WARNING: '预警事件',
  PLAN: '巡检计划',
  MANUAL: '手动创建',
}
const priorityMap: Record<string, string> = {
  high: '高',
  medium: '中',
  low: '低',
  urgent: '紧急',
}
const priorityColorMap: Record<string, string> = {
  high: '#E53935',
  medium: '#FB8C00',
  low: '#42A5F5',
  urgent: '#D32F2F',
}

function statusLabel(s: string) { return statusMap[s] || s }
function orderTypeLabel(t: string) { return orderTypeMap[t] || t }
function sourceLabel(s: string) { return sourceMap[s] || s }
function priorityLabel(p: string) { return priorityMap[p] || p }
function priorityColor(p: string) { return priorityColorMap[p] || '#909399' }

/* ==================== 统计 ==================== */
const stats = ref<OrderStatistics>({
  pending: 0, assigned: 0, inProgress: 0, completed: 0, closed: 0, total: 0,
})

const statsCards: { key: keyof OrderStatistics; label: string; color: string }[] = [
  { key: 'pending', label: '待派单', color: '#909399' },
  { key: 'assigned', label: '待接单', color: '#FB8C00' },
  { key: 'inProgress', label: '处理中', color: '#1E88E5' },
  { key: 'completed', label: '已完成', color: '#43A047' },
  { key: 'closed', label: '已关闭', color: '#606266' },
]

async function loadStats() {
  try {
    stats.value = await getOrderStatistics()
  } catch { /* ignore */ }
}

/* ==================== 表格 ==================== */
const dateRange = ref<string[]>([])

const {
  loading, dataList, total, query,
  fetch, reset, handlePageChange, handleSizeChange,
} = useTable<OperationOrder>({
  fetchApi: (params) => getOrders(params),
  defaultQuery: {},
  immediate: false,
})

const columns = [
  { prop: 'orderCode', label: '工单编号', width: 140, fixed: 'left' as const },
  { prop: 'title', label: '标题', minWidth: 160 },
  { prop: 'orderType', label: '工单类型', width: 100, align: 'center' as const,
    slot: 'orderType' },
  { prop: 'source', label: '来源', width: 100, align: 'center' as const },
  { prop: 'priority', label: '优先级', width: 90, align: 'center' as const, slot: 'priority' },
  { prop: 'status', label: '状态', width: 100, align: 'center' as const, slot: 'status' },
  { prop: 'dispatcherName', label: '派单人', width: 100 },
  { prop: 'assigneeName', label: '处理人', width: 100 },
  { prop: 'createTime', label: '创建时间', width: 170 },
  { prop: 'action', label: '操作', width: 220, fixed: 'right' as const, slot: 'action' },
]

function handleDateChange(val: string[] | null) {
  if (val && val.length === 2) {
    query.startDate = val[0]
    query.endDate = val[1]
  } else {
    delete query.startDate
    delete query.endDate
  }
  fetch()
}

/* ==================== 详情抽屉 ==================== */
const drawerVisible = ref(false)
const currentOrder = ref<OperationOrder | null>(null)

async function showDetail(row: OperationOrder) {
  try {
    currentOrder.value = await getOrderById(row.orderId)
    drawerVisible.value = true
  } catch {
    ElMessage.error('获取工单详情失败')
  }
}

/* ==================== 派单 ==================== */
const assignDialogVisible = ref(false)
const assignForm = reactive<{ orderId: number; orderCode: string; assigneeId: number | null }>({
  orderId: 0, orderCode: '', assigneeId: null,
})
const staffOptions = ref<OperationStaff[]>([])
const submitLoading = ref(false)

function openAssignDialog(row: OperationOrder) {
  assignForm.orderId = row.orderId
  assignForm.orderCode = row.orderCode
  assignForm.assigneeId = null
  assignDialogVisible.value = true
  loadStaffOptions()
}

async function loadStaffOptions() {
  try {
    const res = await getOperationStaff({ page: 1, size: 200 })
    staffOptions.value = res.records
  } catch { /* ignore */ }
}

async function handleAssign() {
  if (!assignForm.assigneeId) {
    ElMessage.warning('请选择处理人')
    return
  }
  submitLoading.value = true
  try {
    await updateOrder(assignForm.orderId, { assigneeId: assignForm.assigneeId })
    ElMessage.success('派单成功')
    assignDialogVisible.value = false
    fetch()
    loadStats()
  } catch { /* handled by interceptor */ } finally {
    submitLoading.value = false
  }
}

/* ==================== 接单 ==================== */
async function handleAccept(row: OperationOrder) {
  try {
    await ElMessageBox.confirm(`确认接受工单「${row.orderCode}」？`, '接单确认', { type: 'info' })
    await acceptOrder(row.orderId)
    ElMessage.success('接单成功')
    fetch()
    loadStats()
  } catch { /* cancel or error */ }
}

/* ==================== 完工 ==================== */
const completeDialogVisible = ref(false)
const completeForm = reactive<{ orderId: number; orderCode: string; result: string }>({
  orderId: 0, orderCode: '', result: '',
})

function openCompleteDialog(row: OperationOrder) {
  completeForm.orderId = row.orderId
  completeForm.orderCode = row.orderCode
  completeForm.result = ''
  completeDialogVisible.value = true
}

async function handleComplete() {
  if (!completeForm.result.trim()) {
    ElMessage.warning('请填写处置结果')
    return
  }
  submitLoading.value = true
  try {
    await completeOrder(completeForm.orderId, completeForm.result)
    ElMessage.success('完工登记成功')
    completeDialogVisible.value = false
    fetch()
    loadStats()
  } catch { /* handled by interceptor */ } finally {
    submitLoading.value = false
  }
}

/* ==================== 关闭 ==================== */
async function handleClose(row: OperationOrder) {
  try {
    await ElMessageBox.confirm(`确认关闭工单「${row.orderCode}」？关闭后不可恢复。`, '关闭确认', { type: 'warning' })
    await closeOrder(row.orderId)
    ElMessage.success('工单已关闭')
    fetch()
    loadStats()
  } catch { /* cancel or error */ }
}

/* ==================== 新建工单 ==================== */
const createDialogVisible = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive({
  title: '',
  orderType: '',
  priority: 'medium',
  description: '',
  pipeId: null as number | null,
  deviceId: null as number | null,
})
const createRules: FormRules = {
  title: [{ required: true, message: '请输入工单标题', trigger: 'blur' }],
  orderType: [{ required: true, message: '请选择工单类型', trigger: 'change' }],
  priority: [{ required: true, message: '请选择优先级', trigger: 'change' }],
}

function openCreateDialog() {
  createForm.title = ''
  createForm.orderType = ''
  createForm.priority = 'medium'
  createForm.description = ''
  createForm.pipeId = null
  createForm.deviceId = null
  createDialogVisible.value = true
}

async function handleCreate() {
  if (!createFormRef.value) return
  await createFormRef.value.validate()
  submitLoading.value = true
  try {
    await createOrder({
      title: createForm.title,
      orderType: createForm.orderType,
      priority: createForm.priority,
      description: createForm.description,
      pipeId: createForm.pipeId ?? undefined,
      deviceId: createForm.deviceId ?? undefined,
    })
    ElMessage.success('工单创建成功')
    createDialogVisible.value = false
    fetch()
    loadStats()
  } catch { /* handled by interceptor */ } finally {
    submitLoading.value = false
  }
}

/* ==================== 初始化 ==================== */
onMounted(() => {
  fetch()
  loadStats()
})
</script>

<style scoped lang="scss">
.order-page {
  padding: $spacing-md;
}

.stats-bar {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
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

.priority-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: $border-radius-sm;
  font-size: 12px;
  line-height: 1.5;
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

.timeline-status {
  font-weight: 600;
  margin-right: $spacing-sm;
}

.timeline-operator {
  color: $text-secondary;
  font-size: 12px;
}

.timeline-remark {
  color: $text-regular;
  font-size: 13px;
  margin-top: $spacing-xs;
}
</style>
