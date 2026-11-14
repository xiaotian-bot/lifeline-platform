<template>
  <div class="construction-page">
    <!-- 搜索区 -->
    <div class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
            <el-option label="待开工" :value="0" />
            <el-option label="施工中" :value="1" />
            <el-option label="已完工" :value="2" />
            <el-option label="停复工" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="query.area" placeholder="项目名称/地点/单位" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetch">查询</el-button>
          <el-button @click="reset">重置</el-button>
          <el-button type="primary" plain @click="openCreateDialog">新建项目</el-button>
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
        <template #status="{ row }">
          <StatusTag :status="String(row.status)" :label="row.statusText || statusLabel(row.status)" />
        </template>
        <template #action="{ row }">
          <el-button link type="primary" size="small" @click="goDetail(row)">查看详情</el-button>
          <el-button link type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
          <el-button
            v-if="canAdvance(row.status)"
            link type="warning" size="small"
            @click="handleStatusAdvance(row)"
          >{{ nextStatusLabel(row.status) }}</el-button>
        </template>
      </PageTable>
    </div>

    <!-- 新建/编辑对话框 -->
    <el-dialog v-model="formDialogVisible" :title="isEdit ? '编辑项目' : '新建项目'" width="540px" :close-on-click-modal="false">
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="项目名称" prop="projectName">
          <el-input v-model="form.projectName" placeholder="请输入项目名称" />
        </el-form-item>
        <el-form-item label="施工地点" prop="location">
          <el-input v-model="form.location" placeholder="请输入施工地点" />
        </el-form-item>
        <el-form-item label="施工单位" prop="contractor">
          <el-input v-model="form.contractor" placeholder="请输入施工单位" />
        </el-form-item>
        <el-form-item label="监理负责人" prop="supervisor">
          <el-input v-model="form.supervisor" placeholder="请输入监理负责人" />
        </el-form-item>
        <el-form-item label="计划开工" prop="startDate">
          <el-date-picker v-model="form.startDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="计划完工" prop="endDate">
          <el-date-picker v-model="form.endDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="经度">
          <el-input-number v-model="form.longitude" :precision="6" :step="0.001" style="width: 100%" />
        </el-form-item>
        <el-form-item label="纬度">
          <el-input-number v-model="form.latitude" :precision="6" :step="0.001" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import PageTable from '@/components/common/PageTable.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { useTable } from '@/composables/useTable'
import {
  getConstructionProjects,
  createConstructionProject,
  updateConstructionProject,
  advanceProjectStatus,
} from '@/api/construction'
import type { ConstructionProject } from '@/types/construction.d'

const router = useRouter()

/* ==================== 状态映射 ==================== */
const statusTextMap: Record<number, string> = {
  0: '待开工',
  1: '施工中',
  2: '已完工',
  3: '已停工',
}

function statusLabel(s: number): string {
  return statusTextMap[s] || '未知'
}

function canAdvance(status: number): boolean {
  // 0→1, 1→2, 1→3, 3→1
  return [0, 1, 3].includes(status)
}

function nextStatusLabel(status: number): string {
  const map: Record<number, string> = { 0: '开工', 1: '停工', 3: '复工' }
  if (status === 1) return '完工'
  return map[status] || ''
}

function nextStatusValue(status: number): number {
  const map: Record<number, number> = { 0: 1, 1: 2, 3: 1 }
  return map[status] ?? status
}

/* ==================== 表格 ==================== */
const {
  loading, dataList, total, query,
  fetch, reset, handlePageChange, handleSizeChange,
} = useTable<ConstructionProject>({
  fetchApi: (params) => getConstructionProjects(params),
  defaultQuery: {},
  immediate: true,
})

const columns = [
  { prop: 'projectName', label: '项目名称', minWidth: 180, fixed: 'left' as const },
  { prop: 'location', label: '位置', minWidth: 160 },
  { prop: 'contractor', label: '施工单位', width: 140 },
  { prop: 'supervisor', label: '监理单位', width: 120 },
  { prop: 'startDate', label: '开工日期', width: 120, align: 'center' as const },
  { prop: 'endDate', label: '计划完工', width: 120, align: 'center' as const },
  { prop: 'status', label: '状态', width: 100, align: 'center' as const, slot: 'status' },
  { prop: 'action', label: '操作', width: 220, fixed: 'right' as const, slot: 'action' },
]

/* ==================== 路由跳转 ==================== */
function goDetail(row: ConstructionProject) {
  router.push(`/construction/${row.id}`)
}

/* ==================== 状态推进 ==================== */
async function handleStatusAdvance(row: ConstructionProject) {
  const target = nextStatusValue(row.status)
  const label = nextStatusValue(row.status) === 2 ? '完工' : nextStatusLabel(row.status)
  try {
    await ElMessageBox.confirm(`确认将项目「${row.projectName}」${label}？`, '状态推进', { type: 'warning' })
    await advanceProjectStatus(row.id, target)
    ElMessage.success('状态推进成功')
    fetch()
  } catch { /* cancel or error */ }
}

/* ==================== 新建/编辑 ==================== */
const formDialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number>(0)
const formRef = ref<FormInstance>()
const submitLoading = ref(false)

const form = reactive({
  projectName: '',
  location: '',
  contractor: '',
  supervisor: '',
  startDate: '',
  endDate: '',
  longitude: null as number | null,
  latitude: null as number | null,
})

const formRules: FormRules = {
  projectName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
}

function resetForm() {
  form.projectName = ''
  form.location = ''
  form.contractor = ''
  form.supervisor = ''
  form.startDate = ''
  form.endDate = ''
  form.longitude = null
  form.latitude = null
}

function openCreateDialog() {
  isEdit.value = false
  editId.value = 0
  resetForm()
  formDialogVisible.value = true
}

function openEditDialog(row: ConstructionProject) {
  isEdit.value = true
  editId.value = row.id
  form.projectName = row.projectName
  form.location = row.location || ''
  form.contractor = row.contractor || ''
  form.supervisor = row.supervisor || ''
  form.startDate = row.startDate || ''
  form.endDate = row.endDate || ''
  form.longitude = row.longitude ?? null
  form.latitude = row.latitude ?? null
  formDialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  submitLoading.value = true
  try {
    const payload: Record<string, unknown> = {
      projectName: form.projectName,
      location: form.location,
      contractor: form.contractor,
      supervisor: form.supervisor,
      startDate: form.startDate || undefined,
      endDate: form.endDate || undefined,
      longitude: form.longitude ?? undefined,
      latitude: form.latitude ?? undefined,
    }
    if (isEdit.value) {
      await updateConstructionProject(editId.value, payload)
      ElMessage.success('项目更新成功')
    } else {
      await createConstructionProject(payload)
      ElMessage.success('项目创建成功')
    }
    formDialogVisible.value = false
    fetch()
  } catch { /* handled by interceptor */ } finally {
    submitLoading.value = false
  }
}
</script>

<style scoped lang="scss">
.construction-page {
  padding: $spacing-md;
}
</style>
