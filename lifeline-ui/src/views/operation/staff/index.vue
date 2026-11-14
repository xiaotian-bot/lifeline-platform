<template>
  <div class="staff-page">
    <!-- 搜索区 -->
    <div class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="技能类型">
          <el-select v-model="query.skillType" placeholder="全部" clearable style="width: 130px">
            <el-option label="燃气" value="gas" />
            <el-option label="排水" value="drainage" />
            <el-option label="电力" value="power" />
            <el-option label="通讯" value="comm" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="在岗" value="active" />
            <el-option label="忙碌" value="busy" />
            <el-option label="离线" value="offline" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="姓名/编号" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetch">查询</el-button>
          <el-button @click="reset">重置</el-button>
          <el-button type="primary" plain @click="openDialog()">新增人员</el-button>
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
        <template #skillType="{ row }">
          <span>{{ skillTypeLabel(row.skillType) }}</span>
        </template>
        <template #status="{ row }">
          <StatusTag :status="row.status" :label="staffStatusLabel(row.status)" />
        </template>
        <template #action="{ row }">
          <el-button link type="primary" size="small" @click="openDialog(row)">编辑</el-button>
        </template>
      </PageTable>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑人员' : '新增人员'" width="480px" :close-on-click-modal="false">
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="80px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="技能类型" prop="skillType">
          <el-select v-model="form.skillType" placeholder="请选择" style="width: 100%">
            <el-option label="燃气" value="gas" />
            <el-option label="排水" value="drainage" />
            <el-option label="电力" value="power" />
            <el-option label="通讯" value="comm" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" placeholder="请选择" style="width: 100%">
            <el-option label="在岗" value="active" />
            <el-option label="忙碌" value="busy" />
            <el-option label="离线" value="offline" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
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
import { getOperationStaff, createStaff, updateStaff } from '@/api/operation'
import type { OperationStaff } from '@/types/operation.d'

/* ==================== 映射表 ==================== */
const skillTypeMap: Record<string, string> = { gas: '燃气', drainage: '排水', power: '电力', comm: '通讯' }
const staffStatusMap: Record<string, string> = { active: '在岗', busy: '忙碌', offline: '离线' }

function skillTypeLabel(t: string) { return skillTypeMap[t] || t }
function staffStatusLabel(s: string) { return staffStatusMap[s] || s }

/* ==================== 表格 ==================== */
const {
  loading, dataList, total, query,
  fetch, reset, handlePageChange, handleSizeChange,
} = useTable<OperationStaff>({
  fetchApi: (params) => getOperationStaff(params),
  defaultQuery: {},
  immediate: true,
})

const columns = [
  { prop: 'staffCode', label: '人员编号', width: 130 },
  { prop: 'name', label: '姓名', width: 120 },
  { prop: 'skillType', label: '技能类型', width: 110, align: 'center' as const, slot: 'skillType' },
  { prop: 'phone', label: '联系方式', width: 140 },
  { prop: 'status', label: '状态', width: 100, align: 'center' as const, slot: 'status' },
  { prop: 'createTime', label: '创建时间', width: 170 },
  { prop: 'action', label: '操作', width: 100, fixed: 'right' as const, slot: 'action' },
]

/* ==================== 新增/编辑 ==================== */
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number>(0)
const formRef = ref<FormInstance>()
const submitLoading = ref(false)

const form = reactive({
  name: '',
  skillType: '',
  phone: '',
  status: 'active',
})

const formRules: FormRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  skillType: [{ required: true, message: '请选择技能类型', trigger: 'change' }],
  phone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
}

function openDialog(row?: OperationStaff) {
  if (row) {
    isEdit.value = true
    editId.value = row.staffId
    form.name = row.name
    form.skillType = row.skillType
    form.phone = row.phone
    form.status = row.status
  } else {
    isEdit.value = false
    editId.value = 0
    form.name = ''
    form.skillType = ''
    form.phone = ''
    form.status = 'active'
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updateStaff(editId.value, { ...form })
      ElMessage.success('人员信息更新成功')
    } else {
      await createStaff({ ...form })
      ElMessage.success('人员新增成功')
    }
    dialogVisible.value = false
    fetch()
  } catch { /* handled by interceptor */ } finally {
    submitLoading.value = false
  }
}
</script>

<style scoped lang="scss">
.staff-page {
  padding: $spacing-md;
}
</style>
