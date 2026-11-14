<template>
  <div class="statistics-page">
    <!-- 搜索表单 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="queryForm" inline class="search-form">
        <el-form-item label="监测设备">
          <el-select
            v-model="queryForm.deviceId"
            placeholder="请选择设备"
            filterable
            clearable
            style="width: 200px"
          >
            <el-option
              v-for="d in deviceOptions"
              :key="d.deviceCode"
              :label="`${d.deviceName} (${d.deviceCode})`"
              :value="d.deviceCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="x"
            :shortcuts="dateShortcuts"
            style="width: 380px"
          />
        </el-form-item>
        <el-form-item label="统计粒度">
          <el-select v-model="queryForm.interval" style="width: 120px">
            <el-option label="按小时" value="1h" />
            <el-option label="按6小时" value="6h" />
            <el-option label="按天" value="1d" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" :loading="loading" @click="handleQuery">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 空状态 -->
    <el-empty v-if="!hasData && !loading" description="请选择设备和时间范围后查询统计数据" :image-size="100" />

    <template v-if="hasData || loading">
      <!-- 统计卡片 -->
      <div class="stat-cards" v-loading="loading && !hasData">
        <div v-for="metric in metricDefs" :key="metric.key" class="stat-card">
          <div class="stat-card-header">
            <span class="stat-card-icon" :style="{ background: metric.color }">
              <el-icon :size="16"><component :is="metric.icon" /></el-icon>
            </span>
            <span class="stat-card-title">{{ metric.label }}</span>
          </div>
          <div class="stat-card-body">
            <div class="stat-row">
              <span class="stat-label">最大值</span>
              <span class="stat-value max">{{ getStatValue(metric.key, 'max') }}</span>
            </div>
            <div class="stat-row">
              <span class="stat-label">最小值</span>
              <span class="stat-value min">{{ getStatValue(metric.key, 'min') }}</span>
            </div>
            <div class="stat-row">
              <span class="stat-label">平均值</span>
              <span class="stat-value avg">{{ getStatValue(metric.key, 'avg') }}</span>
            </div>
          </div>
          <div class="stat-card-footer">
            <span class="stat-unit">{{ metric.unit }}</span>
            <span class="stat-samples">{{ totalSamples }} 个样本</span>
          </div>
        </div>
      </div>

      <!-- 图表区域 -->
      <div class="charts-row">
        <el-card shadow="never" class="chart-half">
          <template #header><span class="chart-title">各指标均值对比</span></template>
          <div ref="barChartRef" class="chart-container" />
        </el-card>
        <el-card shadow="never" class="chart-half">
          <template #header><span class="chart-title">数据分布</span></template>
          <div ref="pieChartRef" class="chart-container" />
        </el-card>
      </div>

      <!-- 统计表格 -->
      <el-card shadow="never" class="stats-table-card">
        <template #header>
          <span class="chart-title">分时段统计明细</span>
        </template>
        <el-table :data="statisticsData" stripe border max-height="400" empty-text="暂无数据">
          <el-table-column label="时间" width="180">
            <template #default="{ row }">{{ formatTs(row.ts) }}</template>
          </el-table-column>
          <el-table-column label="样本数" prop="count" width="80" align="right" />
          <el-table-column label="压力均值" width="110" align="right">
            <template #default="{ row }">{{ fmt(row.avgPressure) }}</template>
          </el-table-column>
          <el-table-column label="压力最大" width="110" align="right">
            <template #default="{ row }">{{ fmt(row.maxPressure) }}</template>
          </el-table-column>
          <el-table-column label="压力最小" width="110" align="right">
            <template #default="{ row }">{{ fmt(row.minPressure) }}</template>
          </el-table-column>
          <el-table-column label="温度均值" width="110" align="right">
            <template #default="{ row }">{{ fmt(row.avgTemperature) }}</template>
          </el-table-column>
          <el-table-column label="温度最大" width="110" align="right">
            <template #default="{ row }">{{ fmt(row.maxTemperature) }}</template>
          </el-table-column>
          <el-table-column label="流量均值" width="110" align="right">
            <template #default="{ row }">{{ fmt(row.avgFlow) }}</template>
          </el-table-column>
          <el-table-column label="流量最大" width="110" align="right">
            <template #default="{ row }">{{ fmt(row.maxFlow) }}</template>
          </el-table-column>
          <el-table-column label="振动均值" width="110" align="right">
            <template #default="{ row }">{{ fmt(row.avgVibration) }}</template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { Search, Odometer, Sunny, DataLine, Histogram } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getDevices } from '@/api/pipe'
import { getStatistics } from '@/api/monitor'
import type { MonitorDevice } from '@/types/pipe.d'
import type { SensorStatistics } from '@/types/monitor.d'

const deviceOptions = ref<MonitorDevice[]>([])
const statisticsData = ref<SensorStatistics[]>([])
const loading = ref(false)

const queryForm = ref({
  deviceId: '',
  interval: '1h',
})

const now = Date.now()
const dateRange = ref<[number, number]>([now - 86400000, now])

const dateShortcuts = [
  { text: '最近1小时', value: () => [Date.now() - 3600000, Date.now()] as [number, number] },
  { text: '最近24小时', value: () => [Date.now() - 86400000, Date.now()] as [number, number] },
  { text: '最近7天', value: () => [Date.now() - 7 * 86400000, Date.now()] as [number, number] },
  { text: '最近30天', value: () => [Date.now() - 30 * 86400000, Date.now()] as [number, number] },
]

const barChartRef = ref<HTMLElement>()
const pieChartRef = ref<HTMLElement>()
let barChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

interface MetricDef {
  key: string
  label: string
  unit: string
  color: string
  icon: typeof Odometer
  maxField: string
  minField: string
  avgField: string
}

const metricDefs: MetricDef[] = [
  { key: 'pressure', label: '压力', unit: 'MPa', color: '#1E88E5', icon: Odometer, maxField: 'maxPressure', minField: 'minPressure', avgField: 'avgPressure' },
  { key: 'temperature', label: '温度', unit: '℃', color: '#E53935', icon: Sunny, maxField: 'maxTemperature', minField: 'minTemperature', avgField: 'avgTemperature' },
  { key: 'flow', label: '流量', unit: 'm³/h', color: '#43A047', icon: DataLine, maxField: 'maxFlow', minField: 'minFlow', avgField: 'avgFlow' },
  { key: 'vibration', label: '振动', unit: 'mm/s', color: '#FB8C00', icon: Histogram, maxField: 'maxVibration', minField: 'minVibration', avgField: 'avgVibration' },
]

const hasData = computed(() => statisticsData.value.length > 0)
const totalSamples = computed(() => statisticsData.value.reduce((s, d) => s + (d.count || 0), 0))

function getStatValue(metricKey: string, type: 'max' | 'min' | 'avg'): string {
  const data = statisticsData.value
  if (data.length === 0) return '--'
  const def = metricDefs.find((m) => m.key === metricKey)!
  const field = type === 'max' ? def.maxField : type === 'min' ? def.minField : def.avgField
  const values = data.map((d) => d[field as keyof SensorStatistics] as number | null).filter((v) => v != null)
  if (values.length === 0) return '--'
  if (type === 'max') return Math.max(...values).toFixed(3)
  if (type === 'min') return Math.min(...values).toFixed(3)
  return (values.reduce((a, b) => a + b, 0) / values.length).toFixed(3)
}

async function loadDevices() {
  try {
    const res = await getDevices({ page: 1, size: 200 })
    deviceOptions.value = res.records || []
  } catch {
    deviceOptions.value = []
  }
}

async function handleQuery() {
  if (!queryForm.value.deviceId) return
  if (!dateRange.value || dateRange.value.length < 2) return

  loading.value = true
  try {
    statisticsData.value = await getStatistics({
      deviceId: queryForm.value.deviceId,
      startTime: dateRange.value[0],
      endTime: dateRange.value[1],
      interval: queryForm.value.interval,
    })
    await nextTick()
    renderBarChart()
    renderPieChart()
  } catch {
    statisticsData.value = []
  } finally {
    loading.value = false
  }
}

function renderBarChart() {
  if (!barChartRef.value) return
  if (!barChart) barChart = echarts.init(barChartRef.value)

  const labels = metricDefs.map((m) => m.label)
  const avgValues = metricDefs.map((m) => {
    const vals = statisticsData.value.map((d) => d[m.avgField as keyof SensorStatistics] as number | null).filter((v) => v != null)
    return vals.length ? +(vals.reduce((a, b) => a + b, 0) / vals.length).toFixed(3) : 0
  })
  const maxValues = metricDefs.map((m) => {
    const vals = statisticsData.value.map((d) => d[m.maxField as keyof SensorStatistics] as number | null).filter((v) => v != null)
    return vals.length ? +Math.max(...vals).toFixed(3) : 0
  })
  const minValues = metricDefs.map((m) => {
    const vals = statisticsData.value.map((d) => d[m.minField as keyof SensorStatistics] as number | null).filter((v) => v != null)
    return vals.length ? +Math.min(...vals).toFixed(3) : 0
  })

  barChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['最大值', '平均值', '最小值'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '36px', top: '12px', containLabel: true },
    xAxis: { type: 'category', data: labels, axisLabel: { color: '#606266' } },
    yAxis: { type: 'value', axisLabel: { color: '#606266' }, splitLine: { lineStyle: { color: '#EBEEF5' } } },
    series: [
      { name: '最大值', type: 'bar', data: maxValues, itemStyle: { color: '#E53935', borderRadius: [4, 4, 0, 0] }, barMaxWidth: 30 },
      { name: '平均值', type: 'bar', data: avgValues, itemStyle: { color: '#1E88E5', borderRadius: [4, 4, 0, 0] }, barMaxWidth: 30 },
      { name: '最小值', type: 'bar', data: minValues, itemStyle: { color: '#43A047', borderRadius: [4, 4, 0, 0] }, barMaxWidth: 30 },
    ],
  }, true)
}

function renderPieChart() {
  if (!pieChartRef.value) return
  if (!pieChart) pieChart = echarts.init(pieChartRef.value)

  const data = metricDefs.map((m) => {
    const vals = statisticsData.value.map((d) => d[m.avgField as keyof SensorStatistics] as number | null).filter((v) => v != null)
    const avg = vals.length ? +(vals.reduce((a, b) => a + b, 0) / vals.length).toFixed(3) : 0
    return { name: m.label, value: avg, itemStyle: { color: m.color } }
  })

  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, data: data.map((d) => d.name) },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}\n{d}%' },
        emphasis: { label: { fontSize: 14, fontWeight: 'bold' } },
        data,
      },
    ],
  }, true)
}

function handleResize() {
  barChart?.resize()
  pieChart?.resize()
}

function formatTs(ts: number | null | undefined): string {
  if (!ts) return '--'
  const d = new Date(ts)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function fmt(val: number | null | undefined): string {
  if (val == null) return '--'
  return val.toFixed(3)
}

onMounted(async () => {
  await loadDevices()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  barChart?.dispose()
  barChart = null
  pieChart?.dispose()
  pieChart = null
})
</script>

<style scoped lang="scss">
.statistics-page {
  padding: $spacing-lg;
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
  height: 100%;
}

.search-card {
  flex-shrink: 0;
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: $spacing-md;
}

.stat-card {
  background: #fff;
  border-radius: $border-radius-lg;
  border: 1px solid $border-lighter;
  padding: $spacing-md;
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: $shadow-light;
  }
}

.stat-card-header {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.stat-card-icon {
  width: 32px;
  height: 32px;
  border-radius: $border-radius-base;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.stat-card-title {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
}

.stat-card-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stat-label {
  font-size: 12px;
  color: $text-secondary;
}

.stat-value {
  font-size: 16px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;

  &.max { color: $danger-color; }
  &.min { color: $success-color; }
  &.avg { color: $primary-color; }
}

.stat-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 11px;
  color: $text-placeholder;
  border-top: 1px solid $border-lighter;
  padding-top: $spacing-xs;
}

.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: $spacing-md;
}

.chart-container {
  width: 100%;
  height: 300px;
}

.chart-title {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
}

.stats-table-card {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}
</style>
