<template>
  <div class="manhole-page">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="井室类型">
          <el-select v-model="query.manholeType" placeholder="全部类型" clearable style="width: 140px">
            <el-option v-for="item in manholeTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="编号/名称/地址" clearable style="width: 180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 表格 -->
    <div class="table-card">
      <div class="table-toolbar">
        <el-button type="primary" :icon="Plus" @click="openDialog()">新增井室</el-button>
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
        <template #manholeType="{ row }">
          <el-tag size="small">{{ manholeTypeLabelMap[row.manholeType] || row.manholeType }}</el-tag>
        </template>
        <template #coord="{ row }">
          <span class="coord-text">{{ row.lng?.toFixed(6) }}, {{ row.lat?.toFixed(6) }}</span>
        </template>
        <template #action="{ row }">
          <el-button link type="primary" size="small" @click="openDialog(row)">编辑</el-button>
        </template>
      </PageTable>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑井室' : '新增井室'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="90px">
        <el-form-item label="井室编号" prop="manholeCode">
          <el-input v-model="formData.manholeCode" placeholder="请输入井室编号" />
        </el-form-item>
        <el-form-item label="井室名称" prop="manholeName">
          <el-input v-model="formData.manholeName" placeholder="请输入井室名称" />
        </el-form-item>
        <el-form-item label="井室类型" prop="manholeType">
          <el-select v-model="formData.manholeType" placeholder="请选择" style="width: 100%">
            <el-option v-for="item in manholeTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="管径(mm)" prop="diameter">
          <el-input-number v-model="formData.diameter" :min="100" :max="3000" style="width: 100%" />
        </el-form-item>
        <el-form-item label="深度(m)" prop="depth">
          <el-input-number v-model="formData.depth" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="经度" prop="lng">
              <el-input-number v-model="formData.lng" :precision="6" :step="0.000001" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="纬度" prop="lat">
              <el-input-number v-model="formData.lat" :precision="6" :step="0.000001" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="地址" prop="address">
          <el-input v-model="formData.address" placeholder="请输入地址" />
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
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance } from 'element-plus'
import { useTable } from '@/composables/useTable'
import { getManholes, createManhole } from '@/api/pipe'
import type { Manhole } from '@/types/pipe.d'
import PageTable from '@/components/common/PageTable.vue'

const manholeTypeOptions = [
  { label: '阀门井', value: 'valve' },
  { label: '检查井', value: 'inspection' },
  { label: '消火栓井', value: 'hydrant' },
  { label: '排泥井', value: 'drain' },
  { label: '排气井', value: 'vent' },
  { label: '其他', value: 'other' },
]

const manholeTypeLabelMap: Record<string, string> = Object.fromEntries(
  manholeTypeOptions.map(o => [o.value, o.label])
)

const columns = [
  { prop: 'manholeCode', label: '编号', width: 140, fixed: 'left' as const },
  { prop: 'manholeName', label: '名称', minWidth: 140 },
  { prop: 'manholeType', label: '类型', width: 100, align: 'center' as const, slot: 'manholeType' },
  { prop: 'diameter', label: '管径(mm)', width: 100, align: 'right' as const },
  { prop: 'depth', label: '深度(m)', width: 90, align: 'right' as const },
  { prop: 'coord', label: '坐标', minWidth: 180, slot: 'coord' },
  { prop: 'address', label: '地址', minWidth: 160 },
  { prop: 'action', label: '操作', width: 80, fixed: 'right' as const, align: 'center' as const, slot: 'action' },
]

const { loading, dataList, total, query, fetch, handlePageChange, handleSizeChange } = useTable<Manhole>({
  fetchApi: getManholes,
})

function handleSearch() {
  query.page = 1
  fetch()
}

function handleReset() {
  query.manholeType = undefined
  query.keyword = undefined
  handleSearch()
}

// 对话框
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const defaultForm = () => ({
  manholeCode: '',
  manholeName: '',
  manholeType: '',
  diameter: undefined as number | undefined,
  depth: undefined as number | undefined,
  lng: undefined as number | undefined,
  lat: undefined as number | undefined,
  address: '',
})

const formData = reactive(defaultForm())

const formRules = {
  manholeCode: [{ required: true, message: '请输入井室编号', trigger: 'blur' }],
  manholeType: [{ required: true, message: '请选择井室类型', trigger: 'change' }],
  lng: [{ required: true, message: '请输入经度', trigger: 'blur' }],
  lat: [{ required: true, message: '请输入纬度', trigger: 'blur' }],
}

function openDialog(row?: Manhole) {
  if (row) {
    isEdit.value = true
    Object.assign(formData, {
      manholeCode: row.manholeCode,
      manholeName: row.manholeName || '',
      manholeType: row.manholeType,
      diameter: row.diameter,
      depth: row.depth,
      lng: row.lng,
      lat: row.lat,
      address: row.address || '',
    })
  } else {
    isEdit.value = false
    Object.assign(formData, defaultForm())
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    await createManhole({ ...formData } as Partial<Manhole>)
    ElMessage.success('创建成功')
    dialogVisible.value = false
    fetch()
  } catch {
    // handled
  } finally {
    submitLoading.value = false
  }
}
</script>

<style scoped lang="scss">
.manhole-page {
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

.coord-text {
  font-family: monospace;
  font-size: 12px;
  color: $text-secondary;
}
</style>
