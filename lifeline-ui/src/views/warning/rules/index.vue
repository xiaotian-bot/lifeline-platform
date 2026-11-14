<template>
  <div class="page-container">
    <!-- 搜索表单 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="规则类型">
          <el-select v-model="query.ruleType" placeholder="全部类型" clearable style="width: 150px">
            <el-option v-for="t in ruleTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="管网类型">
          <el-select v-model="query.pipeType" placeholder="全部类型" clearable style="width: 150px">
            <el-option v-for="t in pipeTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.enabled" placeholder="全部状态" clearable style="width: 120px">
            <el-option label="已启用" :value="true" />
            <el-option label="已停用" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetch">查询</el-button>
          <el-button @click="reset">重置</el-button>
          <el-button type="success" @click="handleAdd">新增规则</el-button>
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
      <template #ruleType="{ row }">
        {{ ruleTypeLabelMap[row.ruleType] || row.ruleType }}
      </template>
      <template #conditionExpr="{ row }">
        <code>{{ row.conditionExpr || `${row.condition} ${row.threshold}` }}</code>
      </template>
      <template #warningLevel="{ row }">
        <StatusTag :level="levelCode(row.warningLevel)" :label="levelLabel(row.warningLevel)" />
      </template>
      <template #enabled="{ row }">
        <el-switch
          :model-value="row.enabled"
          @change="(val: boolean) => handleStatusChange(row, val)"
          :loading="statusLoadingId === row.ruleId"
        />
      </template>
      <template #action="{ row }">
        <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
        <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
      </template>
    </PageTable>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="formVisible" :title="formIsEdit ? '编辑规则' : '新增规则'" width="520px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="formData.ruleName" placeholder="请输入规则名称" />
        </el-form-item>
        <el-form-item label="规则类型" prop="ruleType">
          <el-select v-model="formData.ruleType" placeholder="请选择规则类型" style="width: 100%">
            <el-option v-for="t in ruleTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="管网类型" prop="pipeType">
          <el-select v-model="formData.pipeType" placeholder="请选择管网类型" style="width: 100%">
            <el-option v-for="t in pipeTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="条件表达式" prop="conditionExpr">
          <el-input v-model="formData.conditionExpr" placeholder="如: value > threshold" />
        </el-form-item>
        <el-form-item label="阈值" prop="thresholdValue">
          <el-input-number v-model="formData.thresholdValue" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="预警等级" prop="warningLevel">
          <el-select v-model="formData.warningLevel" placeholder="请选择预警等级" style="width: 100%">
            <el-option label="蓝色预警(Ⅳ)" :value="4" />
            <el-option label="黄色预警(Ⅲ)" :value="3" />
            <el-option label="橙色预警(Ⅱ)" :value="2" />
            <el-option label="红色预警(Ⅰ)" :value="1" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="formLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import PageTable from '@/components/common/PageTable.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { useTable } from '@/composables/useTable'
import {
  getWarningRules,
  createWarningRule,
  updateWarningRule,
  changeRuleStatus,
  deleteWarningRule,
} from '@/api/warning'
import type { WarningRule } from '@/types/warning.d'

const pipeTypes = ['供水', '排水', '燃气', '供热', '电力', '通信']
const ruleTypes = [
  { value: 'PRESSURE_LOW', label: '压力过低' },
  { value: 'PRESSURE_HIGH', label: '压力过高' },
  { value: 'FLOW_HIGH', label: '流量过高' },
  { value: 'TEMP_HIGH', label: '温度过高' },
  { value: 'VIBRATION_HIGH', label: '振动过高' },
  { value: 'GAS_CONCENTRATION', label: '气体浓度' },
  { value: 'LEVEL_LOW', label: '液位过低' },
]
const ruleTypeLabelMap: Record<string, string> = Object.fromEntries(ruleTypes.map(t => [t.value, t.label]))

function levelCode(level: number): string {
  const map: Record<number, string> = { 1: 'red', 2: 'orange', 3: 'yellow', 4: 'blue' }
  return map[level] || 'blue'
}
function levelLabel(level: number): string {
  const map: Record<number, string> = { 1: '红色预警', 2: '橙色预警', 3: '黄色预警', 4: '蓝色预警' }
  return map[level] || '未知'
}

// 表格
const { loading, dataList, total, query, fetch, reset, handlePageChange, handleSizeChange } = useTable<WarningRule>({
  fetchApi: getWarningRules,
  immediate: true,
})

const columns = [
  { prop: 'ruleName', label: '规则名称', minWidth: 150 },
  { prop: 'ruleType', label: '规则类型', width: 120, slot: 'ruleType' },
  { prop: 'pipeType', label: '管网类型', width: 100 },
  { prop: 'conditionExpr', label: '条件表达式', minWidth: 160, slot: 'conditionExpr' },
  { prop: 'thresholdValue', label: '阈值', width: 100 },
  { prop: 'warningLevel', label: '预警等级', width: 110, slot: 'warningLevel' },
  { prop: 'enabled', label: '状态', width: 90, slot: 'enabled' },
  { prop: 'action', label: '操作', width: 130, fixed: 'right' as const, slot: 'action' },
]

// 启用/停用
const statusLoadingId = ref<number | null>(null)
async function handleStatusChange(row: WarningRule, val: boolean) {
  statusLoadingId.value = row.ruleId
  try {
    await changeRuleStatus(row.ruleId, val ? 1 : 0)
    ElMessage.success(val ? '已启用' : '已停用')
    fetch()
  } catch {
    // error handled by interceptor
  } finally {
    statusLoadingId.value = null
  }
}

// 删除
async function handleDelete(row: WarningRule) {
  try {
    await ElMessageBox.confirm(`确定删除规则「${row.ruleName}」吗？`, '提示', { type: 'warning' })
    await deleteWarningRule(row.ruleId)
    ElMessage.success('删除成功')
    fetch()
  } catch {
    // cancelled or error
  }
}

// 表单
const formVisible = ref(false)
const formIsEdit = ref(false)
const formLoading = ref(false)
const formRef = ref<FormInstance>()
let editingId = 0

const formData = reactive({
  ruleName: '',
  ruleType: '',
  pipeType: '',
  conditionExpr: '',
  thresholdValue: 0,
  warningLevel: 4,
})

const formRules = {
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  ruleType: [{ required: true, message: '请选择规则类型', trigger: 'change' }],
  thresholdValue: [{ required: true, message: '请输入阈值', trigger: 'blur' }],
  warningLevel: [{ required: true, message: '请选择预警等级', trigger: 'change' }],
}

function handleAdd() {
  formIsEdit.value = false
  editingId = 0
  formData.ruleName = ''
  formData.ruleType = ''
  formData.pipeType = ''
  formData.conditionExpr = ''
  formData.thresholdValue = 0
  formData.warningLevel = 4
  formVisible.value = true
}

function handleEdit(row: WarningRule) {
  formIsEdit.value = true
  editingId = row.ruleId
  formData.ruleName = row.ruleName
  formData.ruleType = row.ruleType
  formData.pipeType = row.pipeType
  formData.conditionExpr = row.conditionExpr || ''
  formData.thresholdValue = row.thresholdValue ?? row.threshold ?? 0
  formData.warningLevel = row.warningLevel ?? 4
  formVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  formLoading.value = true
  try {
    const payload: Partial<WarningRule> = { ...formData }
    if (formIsEdit.value) {
      await updateWarningRule(editingId, payload)
      ElMessage.success('更新成功')
    } else {
      await createWarningRule(payload)
      ElMessage.success('创建成功')
    }
    formVisible.value = false
    fetch()
  } catch {
    // error handled by interceptor
  } finally {
    formLoading.value = false
  }
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

code {
  background: $bg-color;
  padding: 2px 6px;
  border-radius: $border-radius-sm;
  font-size: 12px;
  color: $text-regular;
}
</style>
