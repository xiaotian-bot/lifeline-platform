<template>
  <div class="history-page">
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
            @change="handleQuery"
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
        <el-form-item label="聚合间隔">
          <el-select v-model="queryForm.interval" placeholder="原始数据" clearable style="width: 120px">
            <el-option label="原始" value="" />
            <el-option label="1分钟" value="1m" />
            <el-option label="5分钟" value="5m" />
            <el-option label="1小时" value="1h" />
            <el-option label="1天" value="1d" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" :loading="loading" @click="handleQuery">查询</el-button>
          <el-button :icon="Download" @click="exportCsv" :disabled="historyData.length === 0">导出CSV</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 图表区域 -->
    <el-card shadow="never" class="chart-card" v-loading="loading">
      <div v-if="historyData.length === 0 && !loading" class="chart-empty">
        <el-empty description="请选择设备和时间范围后查询" :image-size="80" />
      </div>
      <div v-else ref="chartRef" class="history-chart" />
    </el-card>

    <!-- 数据表格 -->
    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="table-header">
          <span class="table-title">数据明细</span>
          <span class="data-count">共 {{ historyData.length }} 条</span>
        </div>
      </template>
      <el-table
        :data="pagedData"
        stripe
        border
        style="width: 100%"
        max-height="400"
        v-loading="loading"
        empty-text="暂无数据"
      >
        <el-table-column label="时间" width="180">
          <template #default="{ row }">{{ formatTs(row.ts) }}</template>
        </el-table-column>
        <el-table-column label="设备编码" prop="deviceId" width="140" />
        <el-table-column label="管网类型" prop="pipeType" width="100" />
        <el-table-column label="传感器类型" prop="sensorType" width="120" />
        <el-table-column label="压力 (MPa)" width="120" align="right">
          <template #default="{ row }">{{ fmt(row.pressure) }}</template>
        </el-table-column>
        <el-table-column label="温度 (℃)" width="110" align="right">
          <template #default="{ row }">{{ fmt(row.temperature) }}</template>
        </el-table-column>
        <el-table-column label="流量 (m³/h)" width="120" align="right">
          <template #default="{ row }">{{ fmt(row.flow) }}</template>
        </el-table-column>
        <el-table-column label="振动 (mm/s)" width="120" align="right">
          <template #default="{ row }">{{ fmt(row.vibration) }}</template>
        </el-table-column>
        <el-table-column label="腐蚀度" width="100" align="right">
          <template #default="{ row }">{{ fmt(row.corrosion) }}</template>
        </el-table-column>
        <el-table-column label="泄漏指标" width="110" align="right">
          <template #default="{ row }">{{ fmt(row.leakage) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : row.status === 1 ? 'warning' : 'danger'" size="small">
              {{ row.status === 0 ? '正常' : row.status === 1 ? '异常' : '报警' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[20, 50, 100, 200]"
          :total="historyData.length"
          layout="total, sizes, prev, pager, next, jumper"
          small
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { Search, Download } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getDevices } from '@/api/pipe'
import { getHistoryData } from '@/api/monitor'
import type { MonitorDevice } from '@/types/pipe.d'
import type { SensorData } from '@/types/monitor.d'

const route = useRoute()

const deviceOptions = ref<MonitorDevice[]>([])
const loading = ref(false)
const historyData = ref<SensorData[]>([])
const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null

const queryForm = ref({
  deviceId: '' as string,
  interval: '',
})

const now = Date.now()
const dateRange = ref<[number, number]>([now - 3600000, now])

const currentPage = ref(1)
const pageSize = ref(50)

const dateShortcuts = [
  { text: '最近1小时', value: () => [Date.now() - 3600000, Date.now()] as [number, number] },
  { text: '最近24小时', value: () => [Date.now() - 86400000, Date.now()] as [number, number] },
  { text: '最近7天', value: () => [Date.now() - 7 * 86400000, Date.now()] as [number, number] },
  { text: '最近30天', value: () => [Date.now() - 30 * 86400000, Date.now()] as [number, number] },
]

const pagedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return historyData.value.slice(start, start + pageSize.value)
})

// 加载设备列表
async function loadDevices() {
  try {
    const res = await getDevices({ page: 1, size: 200 })
    deviceOptions.value = res.records || []
  } catch {
    deviceOptions.value = []
  }
}

// 查询历史数据
async function handleQuery() {
  if (!queryForm.value.deviceId) return
  if (!dateRange.value || dateRange.value.length < 2) return

  loading.value = true
  try {
    const params: Record<string, unknown> = {
      deviceId: queryForm.value.deviceId,
      startTime: dateRange.value[0],
      endTime: dateRange.value[1],
      limit: 10000,
    }
    if (queryForm.value.interval) params.interval = queryForm.value.interval
    historyData.value = await getHistoryData(params as any)
    await nextTick()
    renderChart()
  } catch {
    historyData.value = []
  } finally {
    loading.value = false
  }
}

// 渲染图表
function renderChart() {
  if (!chartRef.value) return
  if (!chart) {
    chart = echarts.init(chartRef.value)
  }

  const data = historyData.value
  if (data.length === 0) {
    chart.clear()
    return
  }

  const xData = data.map((d) => formatTs(d.ts))
  const metricKeys = ['pressure', 'temperature', 'flow', 'vibration'] as const
  const metricLabels: Record<string, string> = {
    pressure: '压力 (MPa)',
    temperature: '温度 (℃)',
    flow: '流量 (m³/h)',
    vibration: '振动 (mm/s)',
  }
  const metricColors: Record<string, string> = {
    pressure: '#1E88E5',
    temperature: '#E53935',
    flow: '#43A047',
    vibration: '#FB8C00',
  }

  const series = metricKeys
    .filter((key) => data.some((d) => d[key] != null))
    .map((key) => ({
      name: metricLabels[key],
      type: 'line' as const,
      data: data.map((d) => d[key]),
      smooth: true,
      connectNulls: true,
      lineStyle: { width: 2, color: metricColors[key] },
      itemStyle: { color: metricColors[key] },
      symbol: 'none',
      yAxisIndex: 0,
    }))

  chart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
    },
    legend: {
      data: series.map((s) => s.name),
      bottom: 0,
    },
    grid: { left: '3%', right: '4%', bottom: '40px', top: '16px', containLabel: true },
    xAxis: {
      type: 'category',
      data: xData,
      boundaryGap: false,
      axisLine: { lineStyle: { color: '#DCDFE6' } },
      axisLabel: { color: '#606266', fontSize: 11 },
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#EBEEF5' } },
      axisLabel: { color: '#606266' },
    },
    dataZoom: [
      { type: 'inside', start: 0, end: 100 },
      { type: 'slider', start: 0, end: 100, height: 20, bottom: 28 },
    ],
    series,
  }, true)
}

function handleResize() {
  chart?.resize()
}

// CSV 导出
function exportCsv() {
  const headers = ['时间', '设备编码', '管网类型', '传感器类型', '压力(MPa)', '温度(℃)', '流量(m³/h)', '振动(mm/s)', '腐蚀度', '泄漏指标', '状态']
  const rows = historyData.value.map((d) => [
    formatTs(d.ts),
    d.deviceId,
    d.pipeType,
    d.sensorType,
    d.pressure ?? '',
    d.temperature ?? '',
    d.flow ?? '',
    d.vibration ?? '',
    d.corrosion ?? '',
    d.leakage ?? '',
    d.status === 0 ? '正常' : d.status === 1 ? '异常' : '报警',
  ])
  const csv = '\uFEFF' + [headers.join(','), ...rows.map((r) => r.join(','))].join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `监测数据_${queryForm.value.deviceId}_${formatTs(Date.now())}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

function formatTs(ts: number | null | undefined): string {
  if (!ts) return '--'
  const d = new Date(ts)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function fmt(val: number | null | undefined): string {
  if (val == null) return '--'
  return val.toFixed(3)
}

// 从路由参数初始化
onMounted(async () => {
  await loadDevices()
  const qd = route.query.deviceId as string
  if (qd) {
    queryForm.value.deviceId = qd
    handleQuery()
  }
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
  chart = null
})

watch(() => queryForm.value.deviceId, () => {
  // route query change handled in onMounted
})
</script>

<style scoped lang="scss">
.history-page {
  padding: $spacing-lg;
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
  height: 100%;
}

.search-card {
  flex-shrink: 0;

  .search-form {
    display: flex;
    flex-wrap: wrap;
    gap: 0;
  }
}

.chart-card {
  flex-shrink: 0;

  .chart-empty {
    height: 360px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .history-chart {
    width: 100%;
    height: 400px;
  }
}

.table-card {
  flex: 1;
  min-height: 0;
  overflow: hidden;

  .table-header {
    display: flex;
    align-items: center;
    justify-content: space-between;

    .table-title {
      font-size: 15px;
      font-weight: 600;
      color: $text-primary;
    }

    .data-count {
      font-size: 13px;
      color: $text-secondary;
    }
  }
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: $spacing-md;
}
</style>
