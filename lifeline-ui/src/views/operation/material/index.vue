<template>
  <div class="material-page">
    <!-- 搜索区 -->
    <div class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="物资类型">
          <el-select v-model="query.materialType" placeholder="全部" clearable style="width: 130px">
            <el-option label="工具" value="tool" />
            <el-option label="管材" value="pipe" />
            <el-option label="配件" value="part" />
            <el-option label="安全防护" value="safety" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="物资名称" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetch">查询</el-button>
          <el-button @click="reset">重置</el-button>
          <el-button type="primary" plain @click="openDialog()">新增物资</el-button>
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
        <template #materialType="{ row }">
          <span>{{ materialTypeLabel(row.materialType) }}</span>
        </template>
        <template #stock="{ row }">
          <span :class="{ 'low-stock': row.minStock && row.stock < row.minStock }">
            {{ row.stock }}
            <el-tag v-if="row.minStock && row.stock < row.minStock" type="danger" size="small" style="margin-left: 4px">库存不足</el-tag>
          </span>
        </template>
        <template #action="{ row }">
          <el-button link type="primary" size="small" @click="openDialog(row)">编辑</el-button>
        </template>
      </PageTable>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑物资' : '新增物资'" width="500px" :close-on-click-modal="false">
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="90px">
        <el-form-item label="物资名称" prop="materialName">
          <el-input v-model="form.materialName" placeholder="请输入物资名称" />
        </el-form-item>
        <el-form-item label="物资类型" prop="materialType">
          <el-select v-model="form.materialType" placeholder="请选择" style="width: 100%">
            <el-option label="工具" value="tool" />
            <el-option label="管材" value="pipe" />
            <el-option label="配件" value="part" />
            <el-option label="安全防护" value="safety" />
          </el-select>
        </el-form-item>
        <el-form-item label="库存量" prop="stock">
          <el-input-number v-model="form.stock" :min="0" :max="999999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="单位" prop="unit">
          <el-input v-model="form.unit" placeholder="如: 个、米、套" />
        </el-form-item>
        <el-form-item label="最低库存">
          <el-input-number v-model="form.minStock" :min="0" :max="999999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="存放仓库" prop="warehouse">
          <el-input v-model="form.warehouse" placeholder="请输入仓库名称" />
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
import { useTable } from '@/composables/useTable'
import { getOperationMaterials, createMaterial, updateMaterial } from '@/api/operation'
import type { OperationMaterial } from '@/types/operation.d'

/* ==================== 映射表 ==================== */
const materialTypeMap: Record<string, string> = {
  tool: '工具', pipe: '管材', part: '配件', safety: '安全防护',
}

function materialTypeLabel(t: string) { return materialTypeMap[t] || t }

/* ==================== 表格 ==================== */
const {
  loading, dataList, total, query,
  fetch, reset, handlePageChange, handleSizeChange,
} = useTable<OperationMaterial>({
  fetchApi: (params) => getOperationMaterials(params),
  defaultQuery: {},
  immediate: true,
})

const columns = [
  { prop: 'materialName', label: '物资名称', minWidth: 160 },
  { prop: 'materialType', label: '类型', width: 110, align: 'center' as const, slot: 'materialType' },
  { prop: 'stock', label: '库存量', width: 140, align: 'center' as const, slot: 'stock' },
  { prop: 'unit', label: '单位', width: 80, align: 'center' as const },
  { prop: 'warehouse', label: '仓库', width: 130 },
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
  materialName: '',
  materialType: '',
  stock: 0,
  unit: '',
  minStock: 0,
  warehouse: '',
})

const formRules: FormRules = {
  materialName: [{ required: true, message: '请输入物资名称', trigger: 'blur' }],
  materialType: [{ required: true, message: '请选择物资类型', trigger: 'change' }],
  stock: [{ required: true, message: '请输入库存量', trigger: 'blur' }],
  unit: [{ required: true, message: '请输入单位', trigger: 'blur' }],
  warehouse: [{ required: true, message: '请输入仓库名称', trigger: 'blur' }],
}

function openDialog(row?: OperationMaterial) {
  if (row) {
    isEdit.value = true
    editId.value = row.materialId
    form.materialName = row.materialName
    form.materialType = row.materialType
    form.stock = row.stock
    form.unit = row.unit
    form.minStock = row.minStock || 0
    form.warehouse = row.warehouse
  } else {
    isEdit.value = false
    editId.value = 0
    form.materialName = ''
    form.materialType = ''
    form.stock = 0
    form.unit = ''
    form.minStock = 0
    form.warehouse = ''
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()

  // 库存量校验
  if (form.stock < 0) {
    ElMessage.warning('库存量不能为负数')
    return
  }

  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updateMaterial(editId.value, { ...form })
      ElMessage.success('物资信息更新成功')
    } else {
      await createMaterial({ ...form })
      ElMessage.success('物资新增成功')
    }
    dialogVisible.value = false
    fetch()
  } catch { /* handled by interceptor */ } finally {
    submitLoading.value = false
  }
}
</script>

<style scoped lang="scss">
.material-page {
  padding: $spacing-md;
}

.low-stock {
  color: $danger-color;
  font-weight: 600;
}
</style>
