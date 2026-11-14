<template>
  <div class="inspection-page">
    <!-- 搜索区 -->
    <div class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="计划类型">
          <el-select v-model="query.planType" placeholder="全部" clearable style="width: 130px">
            <el-option label="常规巡检" value="ROUTINE" />
            <el-option label="燃气管网" value="GAS" />
            <el-option label="排水管网" value="DRAINAGE" />
          </el-select>
        </el-form-item>
        <el-form-item label="频率">
          <el-select v-model="query.frequency" placeholder="全部" clearable style="width: 120px">
            <el-option label="每日" value="daily" />
            <el-option label="每周" value="weekly" />
            <el-option label="每月" value="monthly" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="计划名称" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetch">查询</el-button>
          <el-button @click="reset">重置</el-button>
          <el-button type="primary" plain @click="openCreateDialog">新增计划</el-button>
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
        <template #planType="{ row }">
          <span>{{ planTypeLabel(row.planType) }}</span>
        </template>
        <template #frequency="{ row }">
          <span>{{ frequencyLabel(row.frequency) }}</span>
        </template>
        <template #status="{ row }">
          <StatusTag :status="row.status" :label="planStatusLabel(row.status)" />
        </template>
        <template #action="{ row }">
          <el-button link type="primary" size="small" @click="showDetail(row)">详情</el-button>
          <el-button link type="primary" size="small" @click="showTasks(row)">派生工单</el-button>
        </template>
      </PageTable>
    </div>

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawerVisible" title="巡检计划详情" size="520px">
      <template v-if="currentPlan">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="计划名称" :span="2">{{ currentPlan.planName }}</el-descriptions-item>
          <el-descriptions-item label="计划类型">{{ planTypeLabel(currentPlan.planType) }}</el-descriptions-item>
          <el-descriptions-item label="巡检区域">{{ currentPlan.areaName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="频率">{{ frequencyLabel(currentPlan.frequency) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <StatusTag :status="currentPlan.status" :label="planStatusLabel(currentPlan.status)" />
          </el-descriptions-item>
          <el-descriptions-item label="负责人">{{ currentPlan.assigneeName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentPlan.createTime }}</el-descriptions-item>
          <el-descriptions-item label="上次执行">{{ currentPlan.lastExecTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="下次执行">{{ currentPlan.nextExecTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ currentPlan.description || '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>

    <!-- 派生工单抽屉 -->
    <el-drawer v-model="tasksDrawerVisible" title="派生巡检工单" size="620px">
      <el-table v-loading="tasksLoading" :data="tasksList" border stripe>
        <el-table-column prop="orderCode" label="工单编号" width="140" />
        <el-table-column prop="title" label="标题" min-width="160" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <StatusTag :status="row.status" :label="statusLabel(row.status)" />
          </template>
        </el-table-column>
        <el-table-column prop="assigneeName" label="处理人" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
      </el-table>
    </el-drawer>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="createDialogVisible" title="新增巡检计划" width="540px" :close-on-click-modal="false">
      <el-form :model="createForm" :rules="createRules" ref="createFormRef" label-width="90px">
        <el-form-item label="计划名称" prop="planName">
          <el-input v-model="createForm.planName" placeholder="请输入计划名称" />
        </el-form-item>
        <el-form-item label="计划类型" prop="planType">
          <el-select v-model="createForm.planType" placeholder="请选择" style="width: 100%">
            <el-option label="常规巡检" value="ROUTINE" />
            <el-option label="燃气管网" value="GAS" />
            <el-option label="排水管网" value="DRAINAGE" />
          </el-select>
        </el-form-item>
        <el-form-item label="巡检区域" prop="areaId">
          <el-input v-model.number="createForm.areaId" placeholder="区域ID" />
        </el-form-item>
        <el-form-item label="频率" prop="frequency">
          <el-select v-model="createForm.frequency" placeholder="请选择" style="width: 100%">
            <el-option label="每日" value="daily" />
            <el-option label="每周" value="weekly" />
            <el-option label="每月" value="monthly" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" :rows="3" placeholder="请输入计划描述" />
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
import { ref, reactive } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import PageTable from '@/components/common/PageTable.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { useTable } from '@/composables/useTable'
import {
  getInspectionPlans,
  getInspectionPlanById,
  createInspectionPlan,
  getInspectionPlanTasks,
} from '@/api/operation'
import type { InspectionPlan, OperationOrder } from '@/types/operation.d'

/* ==================== 映射表 ==================== */
const planTypeMap: Record<string, string> = { ROUTINE: '常规巡检', GAS: '燃气管网', DRAINAGE: '排水管网' }
const frequencyMap: Record<string, string> = { daily: '每日', weekly: '每周', monthly: '每月' }
const planStatusMap: Record<string, string> = { active: '启用', paused: '暂停', completed: '已完成' }
const statusMap: Record<string, string> = {
  pending: '待派单', assigned: '待接单', 'in-progress': '处理中', completed: '已完成', closed: '已关闭',
}

function planTypeLabel(t: string) { return planTypeMap[t] || t }
function frequencyLabel(f: string) { return frequencyMap[f] || f }
function planStatusLabel(s: string) { return planStatusMap[s] || s }
function statusLabel(s: string) { return statusMap[s] || s }

/* ==================== 表格 ==================== */
const {
  loading, dataList, total, query,
  fetch, reset, handlePageChange, handleSizeChange,
} = useTable<InspectionPlan>({
  fetchApi: (params) => getInspectionPlans(params),
  defaultQuery: {},
  immediate: true,
})

const columns = [
  { prop: 'planName', label: '计划名称', minWidth: 180 },
  { prop: 'planType', label: '计划类型', width: 110, align: 'center' as const, slot: 'planType' },
  { prop: 'areaName', label: '巡检区域', width: 120 },
  { prop: 'frequency', label: '频率', width: 90, align: 'center' as const, slot: 'frequency' },
  { prop: 'status', label: '状态', width: 100, align: 'center' as const, slot: 'status' },
  { prop: 'assigneeName', label: '负责人', width: 100 },
  { prop: 'createTime', label: '创建时间', width: 170 },
  { prop: 'action', label: '操作', width: 160, fixed: 'right' as const, slot: 'action' },
]

/* ==================== 详情抽屉 ==================== */
const drawerVisible = ref(false)
const currentPlan = ref<InspectionPlan | null>(null)

async function showDetail(row: InspectionPlan) {
  try {
    currentPlan.value = await getInspectionPlanById(row.planId)
    drawerVisible.value = true
  } catch {
    ElMessage.error('获取计划详情失败')
  }
}

/* ==================== 派生工单 ==================== */
const tasksDrawerVisible = ref(false)
const tasksLoading = ref(false)
const tasksList = ref<OperationOrder[]>([])

async function showTasks(row: InspectionPlan) {
  tasksDrawerVisible.value = true
  tasksLoading.value = true
  try {
    tasksList.value = await getInspectionPlanTasks(row.planId)
  } catch {
    ElMessage.error('获取派生工单失败')
  } finally {
    tasksLoading.value = false
  }
}

/* ==================== 新增计划 ==================== */
const createDialogVisible = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive({
  planName: '',
  planType: '',
  areaId: null as number | null,
  frequency: '',
  description: '',
})
const createRules: FormRules = {
  planName: [{ required: true, message: '请输入计划名称', trigger: 'blur' }],
  planType: [{ required: true, message: '请选择计划类型', trigger: 'change' }],
  frequency: [{ required: true, message: '请选择频率', trigger: 'change' }],
}
const submitLoading = ref(false)

function openCreateDialog() {
  createForm.planName = ''
  createForm.planType = ''
  createForm.areaId = null
  createForm.frequency = ''
  createForm.description = ''
  createDialogVisible.value = true
}

async function handleCreate() {
  if (!createFormRef.value) return
  await createFormRef.value.validate()
  submitLoading.value = true
  try {
    await createInspectionPlan({
      planName: createForm.planName,
      planType: createForm.planType,
      areaId: createForm.areaId ?? undefined,
      frequency: createForm.frequency,
      description: createForm.description,
    })
    ElMessage.success('巡检计划创建成功')
    createDialogVisible.value = false
    fetch()
  } catch { /* handled by interceptor */ } finally {
    submitLoading.value = false
  }
}
</script>

<style scoped lang="scss">
.inspection-page {
  padding: $spacing-md;
}
</style>
