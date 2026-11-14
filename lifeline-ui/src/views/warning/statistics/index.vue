<template>
  <div class="page-container">
    <!-- 日期选择器 -->
    <el-card shadow="never" class="filter-card">
      <el-form inline>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 300px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 汇总卡片 -->
    <div class="summary-cards">
      <div class="summary-card">
        <div class="summary-card__value">{{ summaryData.total }}</div>
        <div class="summary-card__label">预警总数</div>
      </div>
      <div class="summary-card">
        <div class="summary-card__value">{{ summaryData.confirmedRate }}%</div>
        <div class="summary-card__label">确认率</div>
      </div>
      <div class="summary-card">
        <div class="summary-card__value">{{ summaryData.avgResponseTime }}h</div>
        <div class="summary-card__label">平均响应时间</div>
      </div>
      <div class="summary-card">
        <div class="summary-card__value">{{ summaryData.aiAccuracy }}%</div>
        <div class="summary-card__label">AI准确率</div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-row">
      <el-card shadow="never" class="chart-card">
        <template #header>
          <span class="chart-title">预警等级分布</span>
        </template>
        <div ref="pieLevelRef" class="chart-container"></div>
      </el-card>
      <el-card shadow="never" class="chart-card">
        <template #header>
          <span class="chart-title">管网类型分布</span>
        </template>
        <div ref="barTypeRef" class="chart-container"></div>
      </el-card>
    </div>
    <div class="charts-row">
      <el-card shadow="never" class="chart-card">
        <template #header>
          <span class="chart-title">预警趋势（按日）</span>
        </template>
        <div ref="lineTrendRef" class="chart-container"></div>
      </el-card>
      <el-card shadow="never" class="chart-card">
        <template #header>
          <span class="chart-title">预警来源分布</span>
        </template>
        <div ref="pieSourceRef" class="chart-container"></div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getWarningStatistics } from '@/api/warning'
import type { WarningStatistics } from '@/types/warning.d'

const levelColorMap: Record<string, string> = {
  blue: '#1E88E5',
  yellow: '#FDD835',
  orange: '#FB8C00',
  red: '#E53935',
  '蓝色预警': '#1E88E5',
  '黄色预警': '#FDD835',
  '橙色预警': '#FB8C00',
  '红色预警': '#E53935',
}

const dateRange = ref<string[]>([])

const summaryData = reactive({
  total: 0,
  confirmedRate: 0,
  avgResponseTime: 0,
  aiAccuracy: 0,
})

// 图表 refs
const pieLevelRef = ref<HTMLElement | null>(null)
const barTypeRef = ref<HTMLElement | null>(null)
const lineTrendRef = ref<HTMLElement | null>(null)
const pieSourceRef = ref<HTMLElement | null>(null)

let pieLevelChart: echarts.ECharts | null = null
let barTypeChart: echarts.ECharts | null = null
let lineTrendChart: echarts.ECharts | null = null
let pieSourceChart: echarts.ECharts | null = null

function handleReset() {
  dateRange.value = []
  loadData()
}

async function loadData() {
  try {
    const stats: WarningStatistics = await getWarningStatistics()
    summaryData.total = stats.total || 0

    // 计算确认率
    const confirmedCount = stats.byStatus?.find(s => s.key === 'confirmed' || s.label === '已确认')?.count || 0
    const handlingCount = stats.byStatus?.find(s => s.key === 'handling' || s.label === '处置中')?.count || 0
    const resolvedCount = stats.byStatus?.find(s => s.key === 'resolved' || s.label === '已解除')?.count || 0
    const totalStatus = confirmedCount + handlingCount + resolvedCount
    summaryData.confirmedRate = stats.total > 0 ? Math.round((totalStatus / stats.total) * 100) : 0

    // 模拟数据
    summaryData.avgResponseTime = Math.round(2 + Math.random() * 6)
    summaryData.aiAccuracy = Math.round(75 + Math.random() * 20)

    renderPieLevel(stats)
    renderBarType(stats)
    renderLineTrend()
    renderPieSource()
  } catch {
    // 加载失败时使用模拟数据
    renderWithMockData()
  }
}

function renderPieLevel(stats: WarningStatistics) {
  if (!pieLevelRef.value) return
  if (!pieLevelChart) pieLevelChart = echarts.init(pieLevelRef.value)

  const data = (stats.byLevel || []).map(item => ({
    name: item.label,
    value: item.count,
    itemStyle: { color: levelColorMap[item.key] || levelColorMap[item.label] || '#909399' },
  }))

  pieLevelChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, left: 'center' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{d}%' },
      data,
    }],
  })
}

function renderBarType(stats: WarningStatistics) {
  if (!barTypeRef.value) return
  if (!barTypeChart) barTypeChart = echarts.init(barTypeRef.value)

  const data = stats.byType || []
  barTypeChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 20, bottom: 40 },
    xAxis: { type: 'category', data: data.map(d => d.label), axisLabel: { rotate: 30 } },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      type: 'bar',
      data: data.map(d => d.count),
      barWidth: '50%',
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#42A5F5' },
          { offset: 1, color: '#1E88E5' },
        ]),
        borderRadius: [4, 4, 0, 0],
      },
    }],
  })
}

function renderLineTrend() {
  if (!lineTrendRef.value) return
  if (!lineTrendChart) lineTrendChart = echarts.init(lineTrendRef.value)

  // 生成最近30天模拟数据
  const days = Array.from({ length: 30 }, (_, i) => {
    const d = new Date()
    d.setDate(d.getDate() - (29 - i))
    return `${d.getMonth() + 1}/${d.getDate()}`
  })
  const values = days.map(() => Math.round(5 + Math.random() * 25))

  lineTrendChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 20, bottom: 40 },
    xAxis: { type: 'category', data: days, boundaryGap: false },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      type: 'line',
      data: values,
      smooth: true,
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(30, 136, 229, 0.3)' },
          { offset: 1, color: 'rgba(30, 136, 229, 0.05)' },
        ]),
      },
      lineStyle: { color: '#1E88E5', width: 2 },
      itemStyle: { color: '#1E88E5' },
    }],
  })
}

function renderPieSource() {
  if (!pieSourceRef.value) return
  if (!pieSourceChart) pieSourceChart = echarts.init(pieSourceRef.value)

  const sourceData = [
    { name: '规则触发', value: Math.round(40 + Math.random() * 30), itemStyle: { color: '#1E88E5' } },
    { name: 'AI识别', value: Math.round(20 + Math.random() * 20), itemStyle: { color: '#43A047' } },
    { name: '人工上报', value: Math.round(5 + Math.random() * 15), itemStyle: { color: '#FB8C00' } },
  ]

  pieSourceChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, left: 'center' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{d}%' },
      data: sourceData,
    }],
  })
}

function renderWithMockData() {
  const mockStats: WarningStatistics = {
    total: 128,
    byLevel: [
      { key: 'blue', label: '蓝色预警', count: 52 },
      { key: 'yellow', label: '黄色预警', count: 38 },
      { key: 'orange', label: '橙色预警', count: 25 },
      { key: 'red', label: '红色预警', count: 13 },
    ],
    byType: [
      { key: '供水', label: '供水', count: 42 },
      { key: '排水', label: '排水', count: 35 },
      { key: '燃气', label: '燃气', count: 28 },
      { key: '供热', label: '供热', count: 23 },
    ],
    byStatus: [
      { key: 'pending', label: '待确认', count: 18 },
      { key: 'confirmed', label: '已确认', count: 45 },
      { key: 'handling', label: '处置中', count: 32 },
      { key: 'resolved', label: '已解除', count: 28 },
      { key: 'ignored', label: '误报', count: 5 },
    ],
  }
  summaryData.total = mockStats.total
  summaryData.confirmedRate = 82
  summaryData.avgResponseTime = 4
  summaryData.aiAccuracy = 89

  renderPieLevel(mockStats)
  renderBarType(mockStats)
  renderLineTrend()
  renderPieSource()
}

function handleResize() {
  pieLevelChart?.resize()
  barTypeChart?.resize()
  lineTrendChart?.resize()
  pieSourceChart?.resize()
}

onMounted(() => {
  nextTick(() => {
    loadData()
  })
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  pieLevelChart?.dispose()
  barTypeChart?.dispose()
  lineTrendChart?.dispose()
  pieSourceChart?.dispose()
})
</script>

<style scoped lang="scss">
.page-container {
  padding: $spacing-md;
}

.filter-card {
  margin-bottom: $spacing-md;

  :deep(.el-card__body) {
    padding-bottom: 0;
  }
}

.summary-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: $spacing-md;
  margin-bottom: $spacing-md;
}

.summary-card {
  background: #fff;
  border-radius: $border-radius-lg;
  padding: $spacing-lg;
  text-align: center;
  box-shadow: $shadow-light;
  border-top: 3px solid $primary-color;

  &__value {
    font-size: 28px;
    font-weight: 700;
    color: $primary-color;
  }

  &__label {
    font-size: 13px;
    color: $text-secondary;
    margin-top: $spacing-xs;
  }
}

.charts-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $spacing-md;
  margin-bottom: $spacing-md;
}

.chart-card {
  :deep(.el-card__header) {
    padding: $spacing-md;
    border-bottom: 1px solid $border-lighter;
  }
}

.chart-title {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
}

.chart-container {
  width: 100%;
  height: 300px;
}
</style>
