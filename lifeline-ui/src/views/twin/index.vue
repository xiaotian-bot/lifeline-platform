<template>
  <div class="page-container">
    <!-- 模型数据概览 -->
    <div class="stat-cards">
      <div v-for="item in modelCards" :key="item.key" class="stat-card">
        <div class="stat-card__icon" :style="{ background: item.bgColor }">
          <el-icon :size="24" color="#fff"><component :is="item.icon" /></el-icon>
        </div>
        <div class="stat-card__body">
          <div class="stat-card__value">{{ item.value }}</div>
          <div class="stat-card__label">{{ item.label }}</div>
        </div>
      </div>
    </div>

    <el-row :gutter="16">
      <!-- 实时映射 -->
      <el-col :span="14">
        <el-card shadow="never" class="section-card">
          <template #header>
            <div class="card-header">
              <span class="card-header__title">实时映射</span>
              <div class="card-header__actions">
                <el-input v-model="deviceIdInput" placeholder="输入设备编码" size="small" style="width: 160px" clearable @keyup.enter="loadRealtime" />
                <el-button type="primary" size="small" :loading="realtimeLoading" @click="loadRealtime">查询</el-button>
              </div>
            </div>
          </template>
          <el-table v-loading="realtimeLoading" :data="realtimeData" border stripe size="small" max-height="360">
            <el-table-column prop="deviceId" label="设备编码" width="130" />
            <el-table-column prop="pipeType" label="管网类型" width="80" align="center" />
            <el-table-column prop="sensorType" label="传感器" width="80" align="center" />
            <el-table-column prop="pressure" label="压力(MPa)" width="90" align="right">
              <template #default="{ row }">{{ row.pressure != null ? row.pressure.toFixed(2) : '-' }}</template>
            </el-table-column>
            <el-table-column prop="temperature" label="温度(℃)" width="90" align="right">
              <template #default="{ row }">{{ row.temperature != null ? row.temperature.toFixed(1) : '-' }}</template>
            </el-table-column>
            <el-table-column prop="flow" label="流量(m³/h)" width="90" align="right">
              <template #default="{ row }">{{ row.flow != null ? row.flow.toFixed(2) : '-' }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="deviceStatusType(row.status)" size="small">{{ deviceStatusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="realtimeResult?.degraded" class="degraded-hint">
            <el-icon color="#FB8C00"><WarningFilled /></el-icon>
            部分数据降级: {{ realtimeResult.degradedSources?.join(', ') }}
          </div>
        </el-card>
      </el-col>

      <!-- 预警定位 -->
      <el-col :span="10">
        <el-card shadow="never" class="section-card">
          <template #header>
            <div class="card-header">
              <span class="card-header__title">预警定位</span>
              <div class="card-header__actions">
                <el-input v-model.number="warningIdInput" placeholder="预警ID" size="small" style="width: 100px" clearable @keyup.enter="loadWarningLocation" />
                <el-button type="primary" size="small" :loading="warningLoading" @click="loadWarningLocation">查询</el-button>
              </div>
            </div>
          </template>
          <div v-if="warningLocationResult" class="warning-list">
            <div
              v-for="item in warningLocationResult.items"
              :key="item.warningId"
              class="warning-item"
            >
              <div class="warning-item__header">
                <el-tag :type="warningLevelType(item.warningLevel)" size="small">{{ item.warningLevel }}</el-tag>
                <span class="warning-item__no">{{ item.warningNo }}</span>
              </div>
              <div class="warning-item__title">{{ item.title }}</div>
              <div class="warning-item__meta">
                <span>{{ item.pipeType }}</span>
                <span v-if="item.pipeName"> | {{ item.pipeName }}</span>
                <span v-if="item.longitude && item.latitude"> | {{ item.longitude.toFixed(4) }}, {{ item.latitude.toFixed(4) }}</span>
              </div>
              <div class="warning-item__status">
                <el-tag :type="deviceStatusType(item.realtimeStatus)" size="small">
                  {{ deviceStatusLabel(item.realtimeStatus) }}
                </el-tag>
                <span class="location-source">定位: {{ item.locationSource }}</span>
              </div>
            </div>
            <el-empty v-if="!warningLocationResult.items?.length" description="暂无预警定位" :image-size="50" />
          </div>
          <el-empty v-else description="输入预警ID查询定位" :image-size="50" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 仿真推演 -->
    <el-card shadow="never" class="section-card sim-card">
      <template #header>
        <div class="card-header">
          <span class="card-header__title">仿真推演</span>
          <el-tag v-if="simTaskId" :type="simStatusType" size="small">{{ simStatusLabel }}</el-tag>
        </div>
      </template>
      <el-form :model="simForm" inline class="sim-form">
        <el-form-item label="场景类型">
          <el-select v-model="simForm.scenarioType" placeholder="选择场景" style="width: 160px">
            <el-option label="泄漏" value="LEAKAGE" />
            <el-option label="爆管" value="BURST" />
            <el-option label="压力异常" value="PRESSURE_ABNORMAL" />
            <el-option label="第三方破坏" value="THIRD_PARTY_DAMAGE" />
          </el-select>
        </el-form-item>
        <el-form-item label="管网类型">
          <el-select v-model="simForm.pipeType" placeholder="选择" clearable style="width: 120px">
            <el-option label="燃气" value="GAS" />
            <el-option label="供水" value="WATER" />
            <el-option label="排水" value="SEWAGE" />
            <el-option label="供热" value="HEAT" />
          </el-select>
        </el-form-item>
        <el-form-item label="预警等级">
          <el-select v-model="simForm.warningLevel" placeholder="选择" clearable style="width: 120px">
            <el-option label="蓝色" value="BLUE" />
            <el-option label="黄色" value="YELLOW" />
            <el-option label="橙色" value="ORANGE" />
            <el-option label="红色" value="RED" />
          </el-select>
        </el-form-item>
        <el-form-item label="管网ID">
          <el-input-number v-model="simForm.pipeId" :min="0" style="width: 120px" controls-position="right" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="simLoading" @click="handleSubmitSim">提交推演</el-button>
        </el-form-item>
      </el-form>

      <!-- 推演结果 -->
      <template v-if="simResult">
        <el-divider />
        <div v-if="simResult.outcome" class="sim-result">
          <el-row :gutter="16">
            <el-col :span="6">
              <div class="sim-metric">
                <div class="sim-metric__label">影响等级</div>
                <el-tag :type="impactLevelType(simResult.outcome.impactLevel)" size="default">
                  {{ impactLevelLabel(simResult.outcome.impactLevel) }}
                </el-tag>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="sim-metric">
                <div class="sim-metric__label">影响半径</div>
                <div class="sim-metric__value">{{ simResult.outcome.impactRadiusMeters }} m</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="sim-metric">
                <div class="sim-metric__label">影响面积</div>
                <div class="sim-metric__value">{{ (simResult.outcome.impactAreaSquareMeters / 10000).toFixed(2) }} 万m²</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="sim-metric">
                <div class="sim-metric__label">结论</div>
                <div class="sim-metric__text">{{ simResult.outcome.summary || '-' }}</div>
              </div>
            </el-col>
          </el-row>
          <div v-if="simResult.outcome.isolationSegments?.length" class="sim-section">
            <h4>建议隔离区段</h4>
            <ul><li v-for="(s, i) in simResult.outcome.isolationSegments" :key="i">{{ s }}</li></ul>
          </div>
          <div v-if="simResult.outcome.suggestions?.length" class="sim-section">
            <h4>处置建议</h4>
            <ul><li v-for="(s, i) in simResult.outcome.suggestions" :key="i">{{ s }}</li></ul>
          </div>
        </div>
        <div v-else-if="simResult.error" class="sim-error">
          <el-alert type="error" :closable="false" :title="simResult.error" show-icon />
        </div>
      </template>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Location, Monitor, Connection, Cpu, WarningFilled } from '@element-plus/icons-vue'
import { getTwinModelData, getRealtimeMapping, getWarningLocation, submitSimulation, querySimulation } from '@/api/twin'
import type { ModelDataVO, RealtimeMappingVO, WarningLocationResultVO, SimulationResultVO } from '@/types/twin.d'

// 模型数据
const modelData = ref<ModelDataVO | null>(null)
const modelCards = computed(() => [
  { key: 'pipe', label: '管线数量', value: modelData.value?.pipeCount ?? '-', icon: 'Connection', bgColor: '#1E88E5' },
  { key: 'device', label: '设备数量', value: modelData.value?.deviceCount ?? '-', icon: 'Monitor', bgColor: '#43A047' },
  { key: 'manhole', label: '井盖数量', value: modelData.value?.manholeCount ?? '-', icon: 'Location', bgColor: '#FB8C00' },
  { key: 'length', label: '管线总长(km)', value: modelData.value ? (modelData.value.totalLengthMeters / 1000).toFixed(1) : '-', icon: 'Cpu', bgColor: '#7B1FA2' },
])

async function loadModelData() {
  try {
    modelData.value = await getTwinModelData()
  } catch { /* ignore */ }
}

// 实时映射
const deviceIdInput = ref('')
const realtimeLoading = ref(false)
const realtimeResult = ref<RealtimeMappingVO | null>(null)
const realtimeData = computed(() => realtimeResult.value?.devices || [])

function deviceStatusType(status: number | null): string {
  if (status === 0) return 'success'
  if (status === 1) return 'warning'
  if (status === 2) return 'danger'
  return 'info'
}
function deviceStatusLabel(status: number | null): string {
  if (status === 0) return '正常'
  if (status === 1) return '异常'
  if (status === 2) return '报警'
  return '离线'
}

async function loadRealtime() {
  const ids = deviceIdInput.value.trim()
  if (!ids) {
    ElMessage.warning('请输入设备编码')
    return
  }
  realtimeLoading.value = true
  try {
    realtimeResult.value = await getRealtimeMapping({
      deviceIds: ids.split(/[,\s]+/).filter(Boolean),
    })
  } catch {
    realtimeResult.value = null
  } finally {
    realtimeLoading.value = false
  }
}

// 预警定位
const warningIdInput = ref<number | null>(null)
const warningLoading = ref(false)
const warningLocationResult = ref<WarningLocationResultVO | null>(null)

function warningLevelType(level: string): string {
  const map: Record<string, string> = { BLUE: '', YELLOW: 'warning', ORANGE: 'warning', RED: 'danger' }
  return map[level] || 'info'
}

async function loadWarningLocation() {
  if (!warningIdInput.value) {
    ElMessage.warning('请输入预警ID')
    return
  }
  warningLoading.value = true
  try {
    warningLocationResult.value = await getWarningLocation({ warningId: warningIdInput.value })
  } catch {
    warningLocationResult.value = null
  } finally {
    warningLoading.value = false
  }
}

// 仿真推演
const simForm = reactive({
  scenarioType: '',
  pipeType: '',
  warningLevel: '',
  pipeId: 0,
})
const simLoading = ref(false)
const simTaskId = ref('')
const simResult = ref<SimulationResultVO | null>(null)
let pollTimer: ReturnType<typeof setInterval> | null = null

const simStatusType = computed(() => {
  const s = simResult.value?.status
  if (s === 'COMPLETED') return 'success'
  if (s === 'FAILED') return 'danger'
  return 'warning'
})
const simStatusLabel = computed(() => {
  const s = simResult.value?.status
  if (s === 'COMPLETED') return '已完成'
  if (s === 'FAILED') return '失败'
  return '运行中'
})

function impactLevelType(level: string): string {
  const map: Record<string, string> = { LOW: 'info', MEDIUM: 'warning', HIGH: 'danger', CRITICAL: 'danger' }
  return map[level] || 'info'
}
function impactLevelLabel(level: string): string {
  const map: Record<string, string> = { LOW: '低', MEDIUM: '中', HIGH: '高', CRITICAL: '严重' }
  return map[level] || level
}

async function handleSubmitSim() {
  if (!simForm.scenarioType) {
    ElMessage.warning('请选择场景类型')
    return
  }
  simLoading.value = true
  simResult.value = null
  try {
    const res = await submitSimulation({
      scenarioType: simForm.scenarioType,
      pipeType: simForm.pipeType || undefined,
      warningLevel: simForm.warningLevel || undefined,
      pipeId: simForm.pipeId || undefined,
    })
    simTaskId.value = res.taskId
    startPolling(res.taskId)
  } catch {
    // error handled
  } finally {
    simLoading.value = false
  }
}

function startPolling(taskId: string) {
  stopPolling()
  pollTimer = setInterval(async () => {
    try {
      const res = await querySimulation(taskId)
      simResult.value = res
      if (res.status === 'COMPLETED' || res.status === 'FAILED') {
        stopPolling()
      }
    } catch {
      stopPolling()
    }
  }, 2000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

onMounted(() => {
  loadModelData()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped lang="scss">
.page-container {
  padding: $spacing-md;
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: $spacing-md;
  margin-bottom: $spacing-md;
}

.stat-card {
  background: #fff;
  border-radius: $border-radius-lg;
  padding: $spacing-lg;
  box-shadow: $shadow-light;
  display: flex;
  align-items: center;
  gap: $spacing-md;
  transition: transform 0.2s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: $shadow-base;
  }

  &__icon {
    width: 48px;
    height: 48px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  &__body { flex: 1; }

  &__value {
    font-size: 24px;
    font-weight: 700;
    color: $text-primary;
    line-height: 1.2;
  }

  &__label {
    font-size: 13px;
    color: $text-secondary;
    margin-top: 2px;
  }
}

.section-card {
  margin-bottom: $spacing-md;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;

  &__title {
    font-size: 15px;
    font-weight: 600;
    color: $text-primary;
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
  }
}

.degraded-hint {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  padding: $spacing-sm;
  font-size: 12px;
  color: $warning-color;
  background: rgba($warning-color, 0.06);
  border-radius: $border-radius-base;
  margin-top: $spacing-sm;
}

.warning-list {
  max-height: 360px;
  overflow-y: auto;
}

.warning-item {
  padding: $spacing-md;
  border-bottom: 1px solid $border-lighter;
  transition: background 0.15s;

  &:hover { background: $bg-color; }
  &:last-child { border-bottom: none; }

  &__header {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    margin-bottom: 4px;
  }

  &__no {
    font-size: 12px;
    color: $text-secondary;
    font-family: monospace;
  }

  &__title {
    font-size: 14px;
    font-weight: 500;
    color: $text-primary;
    margin-bottom: 4px;
  }

  &__meta {
    font-size: 12px;
    color: $text-secondary;
    margin-bottom: 4px;
  }

  &__status {
    display: flex;
    align-items: center;
    gap: $spacing-sm;

    .location-source {
      font-size: 11px;
      color: $text-placeholder;
    }
  }
}

.sim-card {
  .sim-form {
    padding-top: $spacing-sm;
  }
}

.sim-result {
  .sim-metric {
    text-align: center;
    padding: $spacing-md;

    &__label {
      font-size: 13px;
      color: $text-secondary;
      margin-bottom: $spacing-sm;
    }

    &__value {
      font-size: 22px;
      font-weight: 700;
      color: $text-primary;
    }

    &__text {
      font-size: 13px;
      color: $text-regular;
      line-height: 1.5;
    }
  }

  .sim-section {
    margin-top: $spacing-md;

    h4 {
      font-size: 13px;
      font-weight: 600;
      color: $text-primary;
      margin-bottom: $spacing-sm;
      padding-left: $spacing-sm;
      border-left: 3px solid $primary-color;
    }

    ul {
      padding-left: $spacing-lg;
      margin: 0;

      li {
        font-size: 13px;
        color: $text-regular;
        line-height: 1.8;
      }
    }
  }
}

.sim-error {
  padding: $spacing-md 0;
}
</style>
