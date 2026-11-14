<template>
  <div class="device-page">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-form :model="query" inline>
        <el-form-item label="设备类型">
          <el-select v-model="query.deviceType" placeholder="全部类型" clearable style="width: 130px">
            <el-option v-for="item in deviceTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="管网类型">
          <el-select v-model="query.pipeType" placeholder="全部类型" clearable style="width: 120px">
            <el-option v-for="item in pipeTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 110px">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
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
        <el-button type="primary" :icon="Plus" @click="openDialog()">新增设备</el-button>
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
        <template #deviceType="{ row }">
          <div class="device-type-cell">
            <el-icon :size="16" :style="{ color: deviceTypeIconColor[row.deviceType] || '#909399' }">
              <component :is="deviceTypeIcon[row.deviceType] || Monitor" />
            </el-icon>
            <span>{{ deviceTypeLabelMap[row.deviceType] || row.deviceType }}</span>
          </div>
        </template>
        <template #pipeType="{ row }">
          <el-tag v-if="row.pipeType" :color="pipeTypeColorMap[row.pipeType]" effect="dark" size="small" style="border: none; color: #fff">
            {{ pipeTypeLabelMap[row.pipeType] || row.pipeType }}
          </el-tag>
          <span v-else class="text-muted">-</span>
        </template>
        <template #coord="{ row }">
          <span class="coord-text">{{ row.lng?.toFixed(6) }}, {{ row.lat?.toFixed(6) }}</span>
        </template>
        <template #status="{ row }">
          <StatusTag :status="deviceStatusMap[row.status] || 'offline'" :label="deviceStatusLabel[row.status] || String(row.status)" />
        </template>
        <template #action="{ row }">
          <el-button link type="primary" size="small" @click="viewDevice(row)">查看</el-button>
        </template>
      </PageTable>
    </div>

    <!-- 新增设备对话框 -->
    <el-dialog v-model="dialogVisible" title="新增设备" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="90px">
        <el-form-item label="设备编号" prop="deviceCode">
          <el-input v-model="formData.deviceCode" placeholder="请输入设备编号" />
        </el-form-item>
        <el-form-item label="设备名称" prop="deviceName">
          <el-input v-model="formData.deviceName" placeholder="请输入设备名称" />
        </el-form-item>
        <el-form-item label="设备类型" prop="deviceType">
          <el-select v-model="formData.deviceType" placeholder="请选择" style="width: 100%">
            <el-option v-for="item in deviceTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="管网类型">
          <el-select v-model="formData.pipeType" placeholder="请选择" style="width: 100%">
            <el-option v-for="item in pipeTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
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
        <el-form-item label="状态">
          <el-select v-model="formData.status" placeholder="请选择" style="width: 100%">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 查看设备详情 -->
    <el-drawer v-model="drawerVisible" title="设备详情" size="400px">
      <template v-if="currentDevice">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="设备编号">{{ currentDevice.deviceCode }}</el-descriptions-item>
          <el-descriptions-item label="设备名称">{{ currentDevice.deviceName }}</el-descriptions-item>
          <el-descriptions-item label="设备类型">{{ deviceTypeLabelMap[currentDevice.deviceType] || currentDevice.deviceType }}</el-descriptions-item>
          <el-descriptions-item label="管网类型">{{ pipeTypeLabelMap[currentDevice.pipeType || ''] || currentDevice.pipeType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="坐标">{{ currentDevice.lng?.toFixed(6) }}, {{ currentDevice.lat?.toFixed(6) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <StatusTag :status="deviceStatusMap[currentDevice.status] || 'offline'" :label="deviceStatusLabel[currentDevice.status] || String(currentDevice.status)" />
          </el-descriptions-item>
          <el-descriptions-item label="电量">{{ currentDevice.batteryLevel != null ? currentDevice.batteryLevel + '%' : '-' }}</el-descriptions-item>
          <el-descriptions-item label="最近上线">{{ currentDevice.lastOnlineTime || '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { Search, Refresh, Plus, Monitor } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance } from 'element-plus'
import { useTable } from '@/composables/useTable'
import { getDevices, createDevice } from '@/api/pipe'
import type { MonitorDevice } from '@/types/pipe.d'
import PageTable from '@/components/common/PageTable.vue'
import StatusTag from '@/components/common/StatusTag.vue'

const deviceTypeOptions = [
  { label: '压力', value: 'pressure' },
  { label: '温度', value: 'temp' },
  { label: '流量', value: 'flow' },
  { label: '振动', value: 'vibration' },
  { label: '腐蚀', value: 'corrosion' },
  { label: '泄漏', value: 'leakage' },
  { label: '视频', value: 'video' },
]

const deviceTypeLabelMap: Record<string, string> = Object.fromEntries(
  deviceTypeOptions.map(o => [o.value, o.label])
)

const deviceTypeIconColor: Record<string, string> = {
  pressure: '#1E88E5',
  temp: '#E53935',
  flow: '#43A047',
  vibration: '#FB8C00',
  corrosion: '#7E57C2',
  leakage: '#FDD835',
  video: '#606266',
}

const deviceTypeIcon: Record<string, unknown> = {
  pressure: Monitor,
  temp: Monitor,
  flow: Monitor,
  vibration: Monitor,
  corrosion: Monitor,
  leakage: Monitor,
  video: Monitor,
}

const pipeTypeOptions = [
  { label: '燃气', value: 'gas' },
  { label: '给水', value: 'water' },
  { label: '排水', value: 'drainage' },
  { label: '热力', value: 'heating' },
  { label: '雨水', value: 'storm' },
]

const pipeTypeLabelMap: Record<string, string> = {
  gas: '燃气', water: '给水', drainage: '排水', heating: '热力', storm: '雨水',
}

const pipeTypeColorMap: Record<string, string> = {
  gas: '#FB8C00', water: '#1E88E5', drainage: '#43A047', heating: '#E53935', storm: '#7E57C2',
}

const statusOptions = [
  { label: '在线', value: 1 },
  { label: '离线', value: 0 },
  { label: '故障', value: 2 },
]

const deviceStatusMap: Record<number, string> = {
  1: 'online',
  0: 'offline',
  2: 'fault',
}

const deviceStatusLabel: Record<number, string> = {
  1: '在线',
  0: '离线',
  2: '故障',
}

const columns = [
  { prop: 'deviceCode', label: '设备编号', width: 140, fixed: 'left' as const },
  { prop: 'deviceName', label: '设备名称', minWidth: 130 },
  { prop: 'deviceType', label: '设备类型', width: 120, slot: 'deviceType' },
  { prop: 'pipeType', label: '管网类型', width: 100, align: 'center' as const, slot: 'pipeType' },
  { prop: 'pipeName', label: '关联管网', minWidth: 130 },
  { prop: 'coord', label: '坐标', minWidth: 170, slot: 'coord' },
  { prop: 'status', label: '状态', width: 90, align: 'center' as const, slot: 'status' },
  { prop: 'action', label: '操作', width: 80, fixed: 'right' as const, align: 'center' as const, slot: 'action' },
]

const { loading, dataList, total, query, fetch, handlePageChange, handleSizeChange } = useTable<MonitorDevice>({
  fetchApi: getDevices,
})

function handleSearch() {
  query.page = 1
  fetch()
}

function handleReset() {
  query.deviceType = undefined
  query.pipeType = undefined
  query.status = undefined
  handleSearch()
}

// 查看设备
const drawerVisible = ref(false)
const currentDevice = ref<MonitorDevice | null>(null)

function viewDevice(row: MonitorDevice) {
  currentDevice.value = row
  drawerVisible.value = true
}

// 新增设备
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const defaultForm = () => ({
  deviceCode: '',
  deviceName: '',
  deviceType: '',
  pipeType: '',
  lng: undefined as number | undefined,
  lat: undefined as number | undefined,
  status: 1 as number,
})

const formData = reactive(defaultForm())

const formRules = {
  deviceCode: [{ required: true, message: '请输入设备编号', trigger: 'blur' }],
  deviceName: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  deviceType: [{ required: true, message: '请选择设备类型', trigger: 'change' }],
  lng: [{ required: true, message: '请输入经度', trigger: 'blur' }],
  lat: [{ required: true, message: '请输入纬度', trigger: 'blur' }],
}

function openDialog() {
  Object.assign(formData, defaultForm())
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    await createDevice({ ...formData } as Partial<MonitorDevice>)
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
.device-page {
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

.device-type-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}

.coord-text {
  font-family: monospace;
  font-size: 12px;
  color: $text-secondary;
}

.text-muted {
  color: $text-placeholder;
}
</style>
