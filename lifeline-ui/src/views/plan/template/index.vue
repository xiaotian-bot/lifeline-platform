<template>
  <div class="page-container">
    <!-- 搜索表单 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="场景类型">
          <el-select v-model="query.scenarioType" placeholder="全部场景" clearable style="width: 150px">
            <el-option v-for="item in scenarioOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="模板名称" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetch">查询</el-button>
          <el-button @click="reset">重置</el-button>
          <el-button type="primary" plain @click="openCreateDialog">新建模板</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
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
      <template #scenarioType="{ row }">
        {{ scenarioLabelMap[row.scenarioType] || row.scenarioType || '-' }}
      </template>
      <template #status="{ row }">
        <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
          {{ row.status === 1 ? '启用' : '停用' }}
        </el-tag>
      </template>
      <template #content="{ row }">
        <span class="content-ellipsis" :title="row.content">{{ row.content || '-' }}</span>
      </template>
      <template #action="{ row }">
        <el-button link type="primary" size="small" @click="handleView(row)">查看</el-button>
        <el-button link type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
        <el-button
          link
          :type="row.status === 1 ? 'warning' : 'success'"
          size="small"
          @click="handleToggleStatus(row)"
        >
          {{ row.status === 1 ? '停用' : '启用' }}
        </el-button>
      </template>
    </PageTable>

    <!-- 新建/编辑对话框 -->
    <el-dialog
      v-model="formDialogVisible"
      :title="isEdit ? '编辑预案模板' : '新建预案模板'"
      width="580px"
      :close-on-click-modal="false"
    >
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="90px">
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="formData.templateName" placeholder="请输入模板名称" maxlength="128" />
        </el-form-item>
        <el-form-item label="场景类型" prop="scenarioType">
          <el-select v-model="formData.scenarioType" placeholder="请选择场景类型" style="width: 100%">
            <el-option v-for="item in scenarioOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="模板类型">
          <el-select v-model="formData.templateType" placeholder="请选择" style="width: 100%">
            <el-option label="应急处置预案" value="EMERGENCY" />
          </el-select>
        </el-form-item>
        <el-form-item label="预案内容" prop="content">
          <el-input
            v-model="formData.content"
            type="textarea"
            :rows="6"
            placeholder="请输入预案处置要点内容"
            maxlength="4000"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 查看详情抽屉 -->
    <el-drawer v-model="detailDrawerVisible" title="模板详情" size="500px">
      <template v-if="currentTemplate">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="模板名称">{{ currentTemplate.templateName }}</el-descriptions-item>
          <el-descriptions-item label="场景类型">
            {{ scenarioLabelMap[currentTemplate.scenarioType] || currentTemplate.scenarioType || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="模板类型">{{ currentTemplate.templateType || 'EMERGENCY' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="currentTemplate.status === 1 ? 'success' : 'info'" size="small">
              {{ currentTemplate.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentTemplate.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ currentTemplate.updatedAt }}</el-descriptions-item>
        </el-descriptions>
        <div class="drawer-section">
          <h4>预案内容</h4>
          <div class="content-block">{{ currentTemplate.content || '暂无内容' }}</div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import PageTable from '@/components/common/PageTable.vue'
import { useTable } from '@/composables/useTable'
import {
  fetchPlanTemplates,
  fetchPlanTemplateDetail,
  savePlanTemplate,
  updatePlanTemplateById,
  switchPlanTemplateStatus,
} from '@/api/plan'
import type { PlanTemplateItem, PlanTemplateSaveRequest } from '@/types/plan.d'

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

/* ==================== 表格 ==================== */
const {
  loading, dataList, total, query,
  fetch, reset, handlePageChange, handleSizeChange,
} = useTable<PlanTemplateItem>({
  fetchApi: (params) => fetchPlanTemplates(params),
  defaultQuery: {},
  immediate: true,
})

const columns = [
  { prop: 'templateName', label: '模板名称', minWidth: 160 },
  { prop: 'scenarioType', label: '场景类型', width: 120, slot: 'scenarioType' },
  { prop: 'status', label: '状态', width: 80, align: 'center' as const, slot: 'status' },
  { prop: 'content', label: '内容摘要', minWidth: 200, slot: 'content' },
  { prop: 'createdAt', label: '创建时间', width: 170 },
  { prop: 'action', label: '操作', width: 180, fixed: 'right' as const, slot: 'action' },
]

/* ==================== 新建/编辑 ==================== */
const formDialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const submitLoading = ref(false)

const formData = reactive<PlanTemplateSaveRequest>({
  templateName: '',
  templateType: 'EMERGENCY',
  scenarioType: '',
  content: '',
})

const formRules: FormRules = {
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  scenarioType: [{ required: true, message: '请选择场景类型', trigger: 'change' }],
  content: [{ required: true, message: '请输入预案内容', trigger: 'blur' }],
}

function openCreateDialog() {
  isEdit.value = false
  editId.value = null
  formData.templateName = ''
  formData.templateType = 'EMERGENCY'
  formData.scenarioType = ''
  formData.content = ''
  formDialogVisible.value = true
}

function openEditDialog(row: PlanTemplateItem) {
  isEdit.value = true
  editId.value = row.id
  formData.templateName = row.templateName
  formData.templateType = row.templateType || 'EMERGENCY'
  formData.scenarioType = row.scenarioType || ''
  formData.content = row.content || ''
  formDialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  submitLoading.value = true
  try {
    if (isEdit.value && editId.value) {
      await updatePlanTemplateById(editId.value, { ...formData })
      ElMessage.success('模板更新成功')
    } else {
      await savePlanTemplate({ ...formData })
      ElMessage.success('模板创建成功')
    }
    formDialogVisible.value = false
    fetch()
  } catch {
    // handled by interceptor
  } finally {
    submitLoading.value = false
  }
}

/* ==================== 启用/停用 ==================== */
async function handleToggleStatus(row: PlanTemplateItem) {
  const action = row.status === 1 ? '停用' : '启用'
  try {
    await ElMessageBox.confirm(`确认${action}模板「${row.templateName}」？`, `${action}确认`, { type: 'info' })
    await switchPlanTemplateStatus(row.id, row.status === 1 ? 0 : 1)
    ElMessage.success(`模板已${action}`)
    fetch()
  } catch {
    // cancel or error
  }
}

/* ==================== 查看详情 ==================== */
const detailDrawerVisible = ref(false)
const currentTemplate = ref<PlanTemplateItem | null>(null)

async function handleView(row: PlanTemplateItem) {
  try {
    currentTemplate.value = await fetchPlanTemplateDetail(row.id)
  } catch {
    currentTemplate.value = row
  }
  detailDrawerVisible.value = true
}
</script>

<style scoped lang="scss">
.page-container {
  padding: $spacing-md;
}

.search-card {
  margin-bottom: $spacing-md;

  :deep(.el-card__body) {
    padding-bottom: 0;
  }
}

.content-ellipsis {
  display: inline-block;
  max-width: 280px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
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

.content-block {
  background: #f8f9fa;
  border-radius: $border-radius-sm;
  padding: $spacing-md;
  font-size: 13px;
  line-height: 1.8;
  color: $text-regular;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
