<template>
  <div class="pipe-list-page">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="管网类型">
          <el-select v-model="query.pipeType" placeholder="全部类型" clearable style="width: 140px">
            <el-option v-for="item in pipeTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 120px">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="管网编号/名称" clearable style="width: 180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 操作栏 + 表格 -->
    <div class="table-card">
      <div class="table-toolbar">
        <el-button type="primary" :icon="Plus" @click="openDialog()">新增管网</el-button>
        <el-button :icon="Refresh" circle @click="fetch" />
      </div>

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
        <template #pipeType="{ row }">
          <el-tag :color="pipeTypeColorMap[row.pipeType]" effect="dark" size="small" style="border: none; color: #fff">
            {{ pipeTypeLabelMap[row.pipeType] || row.pipeType }}
          </el-tag>
        </template>
        <template #status="{ row }">
          <StatusTag :status="statusMap[row.status] || 'normal'" :label="row.status" />
        </template>
        <template #action="{ row }">
          <el-button link type="primary" size="small" @click="goDetail(row.pipeId)">查看</el-button>
          <el-button link type="primary" size="small" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </PageTable>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑管网' : '新增管网'" width="640px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="管网编号" prop="pipeCode">
              <el-input v-model="formData.pipeCode" placeholder="请输入管网编号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="管网名称" prop="pipeName">
              <el-input v-model="formData.pipeName" placeholder="请输入管网名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="管网类型" prop="pipeType">
              <el-select v-model="formData.pipeType" placeholder="请选择" style="width: 100%">
                <el-option v-for="item in pipeTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="管径(mm)" prop="diameter">
              <el-input-number v-model="formData.diameter" :min="1" :max="5000" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="材质" prop="material">
              <el-input v-model="formData.material" placeholder="如：PE、球墨铸铁" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="长度(m)" prop="length">
              <el-input-number v-model="formData.length" :min="0.1" :precision="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="埋深(m)" prop="depth">
              <el-input-number v-model="formData.depth" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="安装日期" prop="installDate">
              <el-date-picker v-model="formData.installDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="formData.status" placeholder="请选择" style="width: 100%">
                <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
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
import { useRouter } from 'vue-router'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { useTable } from '@/composables/useTable'
import { getPipes, createPipe, updatePipe, deletePipe } from '@/api/pipe'
import type { PipeSegment } from '@/types/pipe.d'
import PageTable from '@/components/common/PageTable.vue'
import StatusTag from '@/components/common/StatusTag.vue'

const router = useRouter()

// 管网类型选项
const pipeTypeOptions = [
  { label: '燃气', value: 'gas' },
  { label: '给水', value: 'water' },
  { label: '排水', value: 'drainage' },
  { label: '热力', value: 'heating' },
  { label: '雨水', value: 'storm' },
]

const pipeTypeColorMap: Record<string, string> = {
  gas: '#FB8C00',
  water: '#1E88E5',
  drainage: '#43A047',
  heating: '#E53935',
  storm: '#7E57C2',
}

const pipeTypeLabelMap: Record<string, string> = {
  gas: '燃气',
  water: '给水',
  drainage: '排水',
  heating: '热力',
  storm: '雨水',
}

const statusOptions = [
  { label: '正常', value: '正常' },
  { label: '预警', value: '预警' },
  { label: '故障', value: '故障' },
  { label: '废弃', value: '废弃' },
]

const statusMap: Record<string, string> = {
  '正常': 'normal',
  '预警': 'warning',
  '故障': 'fault',
  '废弃': 'offline',
}

// 表格列配置
const columns = [
  { prop: 'pipeCode', label: '管网编号', width: 140, fixed: 'left' as const },
  { prop: 'pipeName', label: '管网名称', minWidth: 140 },
  { prop: 'pipeType', label: '管网类型', width: 100, align: 'center' as const, slot: 'pipeType' },
  { prop: 'diameter', label: '管径(mm)', width: 100, align: 'right' as const },
  { prop: 'material', label: '材质', width: 120 },
  { prop: 'length', label: '长度(m)', width: 100, align: 'right' as const },
  { prop: 'status', label: '状态', width: 90, align: 'center' as const, slot: 'status' },
  { prop: 'action', label: '操作', width: 160, fixed: 'right' as const, align: 'center' as const, slot: 'action' },
]

// 使用 useTable composable
const { loading, dataList, total, query, fetch, handlePageChange, handleSizeChange } = useTable<PipeSegment>({
  fetchApi: getPipes,
})

function handleSearch() {
  query.page = 1
  fetch()
}

function handleReset() {
  query.pipeType = undefined
  query.status = undefined
  query.keyword = undefined
  handleSearch()
}

// 跳转详情
function goDetail(pipeId: number) {
  router.push(`/asset/pipe/${pipeId}`)
}

// 删除
async function handleDelete(row: PipeSegment) {
  try {
    await ElMessageBox.confirm(`确定删除管网「${row.pipeCode}」吗？删除后不可恢复。`, '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deletePipe(row.pipeId)
    ElMessage.success('删除成功')
    fetch()
  } catch {
    // 用户取消
  }
}

// 对话框
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const defaultForm = () => ({
  pipeCode: '',
  pipeName: '',
  pipeType: '',
  diameter: undefined as number | undefined,
  material: '',
  length: undefined as number | undefined,
  depth: undefined as number | undefined,
  installDate: '',
  status: '正常',
})

const formData = reactive(defaultForm())

const formRules = {
  pipeCode: [{ required: true, message: '请输入管网编号', trigger: 'blur' }],
  pipeName: [{ required: true, message: '请输入管网名称', trigger: 'blur' }],
  pipeType: [{ required: true, message: '请选择管网类型', trigger: 'change' }],
  diameter: [{ required: true, message: '请输入管径', trigger: 'blur' }],
  material: [{ required: true, message: '请输入材质', trigger: 'blur' }],
  length: [{ required: true, message: '请输入长度', trigger: 'blur' }],
  depth: [{ required: true, message: '请输入埋深', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

function openDialog(row?: PipeSegment) {
  if (row) {
    isEdit.value = true
    editId.value = row.pipeId
    Object.assign(formData, {
      pipeCode: row.pipeCode,
      pipeName: row.pipeName || '',
      pipeType: row.pipeType,
      diameter: row.diameter,
      material: row.material,
      length: row.length,
      depth: row.depth,
      installDate: row.installDate || '',
      status: row.status,
    })
  } else {
    isEdit.value = false
    editId.value = null
    Object.assign(formData, defaultForm())
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (isEdit.value && editId.value) {
      await updatePipe(editId.value, { ...formData } as Partial<PipeSegment>)
      ElMessage.success('更新成功')
    } else {
      await createPipe({ ...formData } as Partial<PipeSegment>)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetch()
  } catch {
    // error handled by interceptor
  } finally {
    submitLoading.value = false
  }
}
</script>

<style scoped lang="scss">
.pipe-list-page {
  padding: $spacing-md;
}

.table-card {
  background: #fff;
  border-radius: $border-radius-lg;
  padding: $spacing-md;
}

.table-toolbar {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: $spacing-md;
}
</style>
