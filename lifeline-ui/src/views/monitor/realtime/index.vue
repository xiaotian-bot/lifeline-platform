<template>
  <div class="realtime-panel">
    <!-- 顶部工具栏 -->
    <div class="panel-header">
      <div class="header-left">
        <h2 class="page-title">实时监测面板</h2>
        <div class="connection-badge" :class="monitorStore.connected ? 'is-connected' : 'is-disconnected'">
          <span class="badge-dot" />
          {{ monitorStore.connected ? '已连接' : '未连接' }}
        </div>
      </div>
      <div class="header-right">
        <el-select v-model="selectedArea" placeholder="选择监测区域" @change="handleAreaChange">
          <el-option label="全部区域" value="" />
          <el-option label="AREA-001 城区供水" value="AREA-001" />
          <el-option label="AREA-002 工业园区" value="AREA-002" />
          <el-option label="AREA-003 燃气管网" value="AREA-003" />
          <el-option label="AREA-004 排水系统" value="AREA-004" />
        </el-select>
        <el-button :icon="Refresh" circle @click="refreshDevices" :loading="loadingDevices" />
      </div>
    </div>

    <!-- 加载骨架屏 -->
    <div v-if="loadingInitial" class="card-grid">
      <div v-for="i in 8" :key="i" class="skeleton-card">
        <el-skeleton animated>
          <template #template>
            <el-skeleton-item variant="text" style="width: 60%; height: 20px" />
            <el-skeleton-item variant="p" style="width: 40%; height: 14px; margin-top: 12px" />
            <div style="display: flex; gap: 16px; margin-top: 16px">
              <el-skeleton-item variant="text" style="width: 60px; height: 36px" />
              <el-skeleton-item variant="text" style="width: 60px; height: 36px" />
              <el-skeleton-item variant="text" style="width: 60px; height: 36px" />
            </div>
          </template>
        </el-skeleton>
      </div>
    </div>

    <!-- 空状态 -->
    <el-empty v-else-if="filteredDevices.length === 0" description="暂无监测设备" :image-size="120" />

    <!-- 设备卡片网格 -->
    <div v-else class="card-grid">
      <div
        v-for="device in filteredDevices"
        :key="device.deviceCode"
        class="device-card"
        :class="getCardClass(device)"
        @click="goToHistory(device)"
      >
        <div class="card-top">
          <div class="device-info">
            <span class="device-type-icon" :class="getDeviceTypeClass(device)">
              <el-icon :size="18"><component :is="getDeviceIcon(device)" /></el-icon>
            </span>
            <div class="device-meta">
              <span class="device-name">{{ device.deviceName || device.deviceCode }}</span>
              <span class="device-code">{{ device.deviceCode }}</span>
            </div>
          </div>
          <span class="status-dot" :class="getStatusClass(device)" />
        </div>

        <div class="card-sensor-type">
          <el-tag size="small" :type="getSensorTagType(device)">{{ getSensorTypeLabel(device) }}</el-tag>
          <span class="pipe-type-label">{{ getPipeTypeLabel(device) }}</span>
        </div>

        <div class="card-values">
          <template v-if="getDeviceSensorData(device)">
            <div v-for="metric in getActiveMetrics(device)" :key="metric.key" class="metric-item">
              <span class="metric-label">{{ metric.label }}</span>
              <span class="metric-value" :class="getMetricColorClass(device, metric.key)">
                {{ formatMetricValue(device, metric.key) }}
              </span>
              <span class="metric-unit">{{ metric.unit }}</span>
            </div>
          </template>
          <template v-else>
            <div class="no-data-hint">暂无数据</div>
          </template>
        </div>

        <div class="card-footer">
          <span class="update-time">
            {{ formatTime(getDeviceSensorData(device)?.ts) }}
          </span>
          <span class="battery" v-if="device.batteryLevel != null">
            <el-icon :size="14"><component :is="BatteryIcon" /></el-icon>
            {{ device.batteryLevel }}%
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh, Sunny, Cloudy, Odometer, DataLine, CircleCheck } from '@element-plus/icons-vue'
import { getDevices } from '@/api/pipe'
import { getRealtimeData } from '@/api/monitor'
import { useWebSocket } from '@/composables/useWebSocket'
import { useMonitorStore } from '@/stores/monitor'
import type { MonitorDevice } from '@/types/pipe.d'
import type { SensorData } from '@/types/monitor.d'

const router = useRouter()
const monitorStore = useMonitorStore()

const selectedArea = ref('')
const devices = shallowRef<MonitorDevice[]>([])
const loadingDevices = ref(false)
const loadingInitial = ref(true)

// WebSocket 连接
const { connect, disconnect } = useWebSocket({
  areaId: 0,
  autoConnect: false,
})

// 区域过滤后的设备
const filteredDevices = computed(() => {
  if (!selectedArea.value) return devices.value
  return devices.value.filter((d) => {
    const sd = getDeviceSensorData(d)
    return sd ? sd.areaId === selectedArea.value : true
  })
})

// 设备数据映射
const sensorDataMap = computed(() => {
  const map = new Map<string, SensorData>()
  for (const sd of monitorStore.realtimeData) {
    map.set(sd.deviceId, sd)
  }
  return map
})

function getDeviceSensorData(device: MonitorDevice): SensorData | undefined {
  return sensorDataMap.value.get(device.deviceCode)
}

// 加载设备列表
async function loadDevices() {
  loadingDevices.value = true
  try {
    const res = await getDevices({ page: 1, size: 200 })
    devices.value = res.records || []
  } catch {
    devices.value = []
  } finally {
    loadingDevices.value = false
    loadingInitial.value = false
  }
}

// 加载每个设备的实时数据
async function loadAllRealtimeData() {
  const promises = devices.value.map((d) =>
    getRealtimeData(d.deviceCode).catch(() => null)
  )
  const results = await Promise.allSettled(promises)
  const dataList: SensorData[] = []
  for (const r of results) {
    if (r.status === 'fulfilled' && r.value) {
      dataList.push(r.value)
    }
  }
  monitorStore.setRealtimeData(dataList)
}

function refreshDevices() {
  loadDevices()
}

function handleAreaChange() {
  disconnect()
  const areaNum = selectedArea.value ? parseInt(selectedArea.value.replace('AREA-', '')) : 0
  // reconnect with new area
  connect()
  void areaNum
}

// 初始化
onMounted(async () => {
  await loadDevices()
  await loadAllRealtimeData()
  connect()
})

// 导航到历史页面
function goToHistory(device: MonitorDevice) {
  router.push({ path: '/monitor/history', query: { deviceId: device.deviceCode } })
}

// 显示指标
interface MetricDef { key: string; label: string; unit: string }
const METRICS: MetricDef[] = [
  { key: 'pressure', label: '压力', unit: 'MPa' },
  { key: 'temperature', label: '温度', unit: '℃' },
  { key: 'flow', label: '流量', unit: 'm³/h' },
  { key: 'vibration', label: '振动', unit: 'mm/s' },
  { key: 'corrosion', label: '腐蚀', unit: '' },
  { key: 'leakage', label: '泄漏', unit: '%LEL' },
]

function getActiveMetrics(device: MonitorDevice): MetricDef[] {
  const sd = getDeviceSensorData(device)
  if (!sd) return METRICS.slice(0, 3)
  return METRICS.filter((m) => sd[m.key as keyof SensorData] != null)
}

function formatMetricValue(device: MonitorDevice, key: string): string {
  const sd = getDeviceSensorData(device)
  if (!sd) return '--'
  const val = sd[key as keyof SensorData]
  if (val == null) return '--'
  return typeof val === 'number' ? val.toFixed(2) : String(val)
}

function getMetricColorClass(device: MonitorDevice, key: string): string {
  const sd = getDeviceSensorData(device)
  if (!sd || sd.status === 0) return 'color-normal'
  if (sd.status === 1) return 'color-warning'
  return 'color-danger'
}

function getStatusClass(device: MonitorDevice): string {
  if (device.status === 1) return 'status-online'
  if (device.status === 2) return 'status-fault'
  return 'status-offline'
}

function getCardClass(device: MonitorDevice): string {
  const sd = getDeviceSensorData(device)
  if (device.status === 0) return 'card-offline'
  if (!sd) return ''
  if (sd.status === 2) return 'card-alarm'
  if (sd.status === 1) return 'card-warning'
  return ''
}

function getDeviceIcon(device: MonitorDevice): typeof Sunny {
  const t = device.deviceType || ''
  if (t.includes('燃气')) return Sunny
  if (t.includes('供水') || t.includes('给水')) return DataLine
  if (t.includes('排水')) return Cloudy
  return Odometer
}

function getDeviceTypeClass(device: MonitorDevice): string {
  const t = device.deviceType || ''
  if (t.includes('燃气')) return 'type-gas'
  if (t.includes('供水') || t.includes('给水')) return 'type-water'
  if (t.includes('排水')) return 'type-drain'
  return 'type-default'
}

function getSensorTypeLabel(device: MonitorDevice): string {
  const sd = getDeviceSensorData(device)
  return sd?.sensorType || device.deviceType || '传感器'
}

function getSensorTagType(device: MonitorDevice): '' | 'success' | 'warning' | 'danger' | 'info' {
  const sd = getDeviceSensorData(device)
  if (!sd || sd.status === 0) return 'success'
  if (sd.status === 1) return 'warning'
  return 'danger'
}

function getPipeTypeLabel(device: MonitorDevice): string {
  const sd = getDeviceSensorData(device)
  return sd?.pipeType || ''
}

function formatTime(ts?: number | null): string {
  if (!ts) return '暂无更新'
  const d = new Date(ts)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const BatteryIcon = CircleCheck
</script>

<style scoped lang="scss">
.realtime-panel {
  padding: $spacing-lg;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;

  .header-left {
    display: flex;
    align-items: center;
    gap: $spacing-md;
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
  }
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: $text-primary;
  margin: 0;
}

.connection-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 10px;
  border-radius: $border-radius-round;
  font-size: 12px;

  .badge-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
  }

  &.is-connected {
    background: rgba($success-color, 0.1);
    color: $success-color;
    .badge-dot { background: $success-color; animation: pulse 2s infinite; }
  }
  &.is-disconnected {
    background: rgba($danger-color, 0.1);
    color: $danger-color;
    .badge-dot { background: $danger-color; }
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: $spacing-md;
  overflow-y: auto;
  flex: 1;
}

.skeleton-card {
  padding: $spacing-md;
  background: #fff;
  border-radius: $border-radius-lg;
  border: 1px solid $border-lighter;
}

.device-card {
  background: #fff;
  border-radius: $border-radius-lg;
  border: 1px solid $border-lighter;
  padding: $spacing-md;
  cursor: pointer;
  transition: all 0.25s ease;
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;

  &:hover {
    box-shadow: $shadow-base;
    transform: translateY(-2px);
    border-color: $primary-light;
  }

  &.card-warning {
    border-left: 3px solid $warning-color;
  }
  &.card-alarm {
    border-left: 3px solid $danger-color;
    animation: alarm-flash 1.5s ease-in-out infinite;
  }
  &.card-offline {
    opacity: 0.55;
  }
}

@keyframes alarm-flash {
  0%, 100% { background: #fff; }
  50% { background: rgba($danger-color, 0.04); }
}

.card-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.device-info {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.device-type-icon {
  width: 36px;
  height: 36px;
  border-radius: $border-radius-base;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;

  &.type-gas { background: linear-gradient(135deg, #FB8C00, #F57C00); }
  &.type-water { background: linear-gradient(135deg, #1E88E5, #1565C0); }
  &.type-drain { background: linear-gradient(135deg, #78909C, #546E7A); }
  &.type-default { background: linear-gradient(135deg, #43A047, #2E7D32); }
}

.device-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.device-name {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
}

.device-code {
  font-size: 11px;
  color: $text-secondary;
  font-family: monospace;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 4px;

  &.status-online { background: $success-color; box-shadow: 0 0 6px rgba($success-color, 0.5); }
  &.status-offline { background: $text-placeholder; }
  &.status-fault { background: $danger-color; box-shadow: 0 0 6px rgba($danger-color, 0.5); }
}

.card-sensor-type {
  display: flex;
  align-items: center;
  gap: $spacing-sm;

  .pipe-type-label {
    font-size: 12px;
    color: $text-secondary;
  }
}

.card-values {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-sm $spacing-md;
  min-height: 40px;
}

.metric-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 56px;
}

.metric-label {
  font-size: 11px;
  color: $text-secondary;
}

.metric-value {
  font-size: 18px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  line-height: 1.3;

  &.color-normal { color: $success-color; }
  &.color-warning { color: $warning-color; }
  &.color-danger { color: $danger-color; }
}

.metric-unit {
  font-size: 10px;
  color: $text-placeholder;
}

.no-data-hint {
  font-size: 13px;
  color: $text-placeholder;
  width: 100%;
  text-align: center;
  padding: $spacing-sm 0;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 11px;
  color: $text-secondary;
  border-top: 1px solid $border-lighter;
  padding-top: $spacing-sm;

  .battery {
    display: flex;
    align-items: center;
    gap: 2px;
  }
}
</style>
