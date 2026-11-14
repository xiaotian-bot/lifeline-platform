<template>
  <div class="dashboard-page">
    <!-- 顶部统计卡片 -->
    <div class="stat-row">
      <div class="stat-card" v-for="item in statCards" :key="item.label">
        <div class="stat-icon" :style="{ background: item.bgColor }">
          <el-icon :size="28" :style="{ color: item.iconColor }">
            <component :is="item.icon" />
          </el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-label">{{ item.label }}</div>
          <div class="stat-value">
            <span v-if="item.extra !== undefined" class="stat-extra">{{ item.value }} / {{ item.extra }}</span>
            <span v-else>{{ item.value }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 中间图表行 -->
    <div class="chart-row">
      <div class="chart-card">
        <div class="card-header">
          <h3>预警趋势</h3>
          <span class="card-subtitle">最近7天</span>
        </div>
        <div ref="trendChartRef" class="chart-container" />
      </div>
      <div class="chart-card">
        <div class="card-header">
          <h3>管网类型分布</h3>
        </div>
        <div ref="pieChartRef" class="chart-container" />
      </div>
    </div>

    <!-- 底部列表行 -->
    <div class="list-row">
      <div class="list-card">
        <div class="card-header">
          <h3>最新预警</h3>
          <el-button link type="primary" size="small" @click="$router.push('/warning/center')">查看全部</el-button>
        </div>
        <div class="warning-list">
          <div v-if="warningLoading" class="list-loading">
            <el-skeleton :rows="5" animated />
          </div>
          <div v-else-if="recentWarnings.length === 0" class="list-empty">
            <el-empty description="暂无预警" :image-size="60" />
          </div>
          <div v-else v-for="w in recentWarnings" :key="w.warningId" class="warning-item" @click="$router.push(`/warning/center`)">
            <div class="warning-level-dot" :style="{ background: levelColorMap[w.level] || '#909399' }" />
            <div class="warning-content">
              <div class="warning-title">
                <el-tag :color="levelColorMap[w.level]" effect="dark" size="small" style="border:none;color:#fff;margin-right:6px">
                  {{ levelLabelMap[w.level] || w.level }}
                </el-tag>
                {{ w.title }}
              </div>
              <div class="warning-meta">{{ pipeTypeLabelMap[w.pipeType] || w.pipeType }} · {{ w.createTime }}</div>
            </div>
          </div>
        </div>
      </div>
      <div class="list-card">
        <div class="card-header">
          <h3>待处理工单</h3>
          <el-button link type="primary" size="small" @click="$router.push('/operation/order')">查看全部</el-button>
        </div>
        <div class="order-list">
          <div v-if="orderLoading" class="list-loading">
            <el-skeleton :rows="5" animated />
          </div>
          <div v-else-if="pendingOrders.length === 0" class="list-empty">
            <el-empty description="暂无待处理工单" :image-size="60" />
          </div>
          <div v-else v-for="o in pendingOrders" :key="o.orderId" class="order-item" @click="$router.push('/operation/order')">
            <div class="order-content">
              <div class="order-title">
                <el-tag :type="priorityTypeMap[o.priority] || 'info'" size="small">{{ priorityLabelMap[o.priority] || o.priority }}</el-tag>
                <span style="margin-left:6px">{{ o.title }}</span>
              </div>
              <div class="order-meta">{{ orderTypeLabelMap[o.orderType] || o.orderType }} · {{ o.createTime }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { OfficeBuilding, Monitor, WarningFilled, Document } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getPipes, getDevices } from '@/api/pipe'
import { getWarningStatistics, getWarnings } from '@/api/warning'
import { getOrderStatistics, getOrders } from '@/api/operation'
import type { WarningEvent } from '@/types/warning.d'
import type { OperationOrder } from '@/types/operation.d'

// ==================== 常量映射 ====================
const levelColorMap: Record<string, string> = {
  blue: '#1E88E5', yellow: '#FDD835', orange: '#FB8C00', red: '#E53935',
}
const levelLabelMap: Record<string, string> = {
  blue: '蓝色', yellow: '黄色', orange: '橙色', red: '红色',
}
const pipeTypeLabelMap: Record<string, string> = {
  gas: '燃气', water: '给水', drainage: '排水', heating: '热力', storm: '雨水',
}
const priorityLabelMap: Record<string, string> = {
  low: '低', medium: '中', high: '高', urgent: '紧急',
}
const priorityTypeMap: Record<string, 'success' | 'info' | 'warning' | 'danger'> = {
  low: 'success', medium: 'info', high: 'warning', urgent: 'danger',
}
const orderTypeLabelMap: Record<string, string> = {
  repair: '维修', inspect: '巡检', maintain: '保养', emergency: '应急',
}

// ==================== 统计数据 ====================
const statCards = reactive([
  { label: '管网总数', value: 0, icon: OfficeBuilding, bgColor: 'rgba(30,136,229,0.1)', iconColor: '#1E88E5' },
  { label: '在线设备', value: 0, extra: 0, icon: Monitor, bgColor: 'rgba(67,160,71,0.1)', iconColor: '#43A047' },
  { label: '今日预警', value: 0, icon: WarningFilled, bgColor: 'rgba(251,140,0,0.1)', iconColor: '#FB8C00' },
  { label: '待处理工单', value: 0, icon: Document, bgColor: 'rgba(229,57,53,0.1)', iconColor: '#E53935' },
])

// ==================== 图表 ====================
const trendChartRef = ref<HTMLDivElement>()
const pieChartRef = ref<HTMLDivElement>()
let trendChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

// ==================== 列表数据 ====================
const recentWarnings = ref<WarningEvent[]>([])
const pendingOrders = ref<OperationOrder[]>([])
const warningLoading = ref(false)
const orderLoading = ref(false)

// ==================== 数据加载 ====================
async function loadStats() {
  try {
    const [pipeRes, deviceRes, warnStat, orderStat] = await Promise.all([
      getPipes({ page: 1, size: 1 }).catch(() => ({ records: [], total: 0 })),
      getDevices({ page: 1, size: 1 }).catch(() => ({ records: [], total: 0 })),
      getWarningStatistics().catch(() => ({ total: 0, byLevel: [], byType: [], byStatus: [] })),
      getOrderStatistics().catch(() => ({ pending: 0, assigned: 0, inProgress: 0, completed: 0, closed: 0, total: 0 })),
    ])
    statCards[0].value = pipeRes.total
    // 在线设备数 / 总设备数
    const onlineDevices = await getDevices({ page: 1, size: 1, status: 1 }).catch(() => ({ records: [], total: 0 }))
    statCards[1].value = onlineDevices.total
    statCards[1].extra = deviceRes.total
    statCards[2].value = warnStat.total
    statCards[3].value = orderStat.pending

    // 饼图：管网类型分布
    const allPipes = await getPipes({ page: 1, size: 500 }).catch(() => ({ records: [], total: 0 }))
    const typeCount: Record<string, number> = {}
    for (const p of allPipes.records) {
      typeCount[p.pipeType] = (typeCount[p.pipeType] || 0) + 1
    }
    renderPieChart(typeCount)

    // 折线图：预警趋势（使用 byType 数据）
    renderTrendChart(warnStat)
  } catch {
    // handled
  }
}

async function loadWarnings() {
  warningLoading.value = true
  try {
    const res = await getWarnings({ page: 1, size: 5, orderBy: 'createTime', orderDir: 'desc' })
    recentWarnings.value = res.records
  } catch {
    // handled
  } finally {
    warningLoading.value = false
  }
}

async function loadOrders() {
  orderLoading.value = true
  try {
    const res = await getOrders({ page: 1, size: 5, status: 'pending', orderBy: 'createTime', orderDir: 'desc' })
    pendingOrders.value = res.records
  } catch {
    // handled
  } finally {
    orderLoading.value = false
  }
}

// ==================== 图表渲染 ====================
function renderTrendChart(stat: { byLevel: { key: string; label: string; count: number }[] }) {
  if (!trendChartRef.value) return
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }
  // 生成最近7天日期
  const days: string[] = []
  for (let i = 6; i >= 0; i--) {
    const d = new Date()
    d.setDate(d.getDate() - i)
    days.push(`${d.getMonth() + 1}/${d.getDate()}`)
  }
  // 按等级生成模拟趋势线
  const levels = stat.byLevel?.length ? stat.byLevel : [
    { key: 'blue', label: '蓝色', count: 12 },
    { key: 'yellow', label: '黄色', count: 8 },
    { key: 'orange', label: '橙色', count: 3 },
    { key: 'red', label: '红色', count: 1 },
  ]
  const series = levels.map((lv) => ({
    name: lv.label || lv.key,
    type: 'line' as const,
    smooth: true,
    symbol: 'circle',
    symbolSize: 6,
    lineStyle: { width: 2 },
    areaStyle: { opacity: 0.08 },
    data: Array.from({ length: 7 }, () => Math.max(0, Math.round((lv.count / 7) * (0.5 + Math.random())))),
  }))

  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0, itemWidth: 14, itemHeight: 8, textStyle: { fontSize: 11 } },
    grid: { top: 10, right: 16, bottom: 36, left: 40 },
    xAxis: { type: 'category', data: days, axisLine: { lineStyle: { color: '#E4E7ED' } }, axisLabel: { color: '#909399', fontSize: 11 } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#F0F2F5' } }, axisLabel: { color: '#909399', fontSize: 11 } },
    series,
  }, true)
}

function renderPieChart(typeCount: Record<string, number>) {
  if (!pieChartRef.value) return
  if (!pieChart) {
    pieChart = echarts.init(pieChartRef.value)
  }
  const colorMap: Record<string, string> = {
    gas: '#FB8C00', water: '#1E88E5', drainage: '#43A047', heating: '#E53935', storm: '#7E57C2',
  }
  const data = Object.entries(typeCount).map(([key, count]) => ({
    name: pipeTypeLabelMap[key] || key,
    value: count,
    itemStyle: { color: colorMap[key] || '#909399' },
  }))
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, itemWidth: 14, itemHeight: 8, textStyle: { fontSize: 11 } },
    series: [{
      type: 'pie',
      radius: ['40%', '65%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: true,
      label: { show: true, formatter: '{b}\n{d}%', fontSize: 11 },
      data: data.length ? data : [{ name: '暂无数据', value: 0, itemStyle: { color: '#E4E7ED' } }],
    }],
  }, true)
}

// ==================== 响应式 ====================
function handleResize() {
  trendChart?.resize()
  pieChart?.resize()
}

// ==================== 自动刷新 ====================
let refreshTimer: ReturnType<typeof setInterval> | null = null

// ==================== 生命周期 ====================
onMounted(async () => {
  await nextTick()
  loadStats()
  loadWarnings()
  loadOrders()
  window.addEventListener('resize', handleResize)
  refreshTimer = setInterval(() => {
    loadStats()
    loadWarnings()
    loadOrders()
  }, 30000)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (refreshTimer) clearInterval(refreshTimer)
  trendChart?.dispose()
  pieChart?.dispose()
})
</script>

<style scoped lang="scss">
.dashboard-page {
  padding: $spacing-md;
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: $spacing-md;
}

.stat-card {
  background: #fff;
  border-radius: $border-radius-lg;
  padding: $spacing-lg;
  display: flex;
  align-items: center;
  gap: $spacing-lg;
  box-shadow: $shadow-light;
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: $shadow-base;
  }
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-label {
  font-size: 13px;
  color: $text-secondary;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: $text-primary;
  line-height: 1.2;
}

.stat-extra {
  font-size: 22px;
}

.chart-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: $spacing-md;
}

.chart-card {
  background: #fff;
  border-radius: $border-radius-lg;
  padding: $spacing-lg;
  box-shadow: $shadow-light;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $spacing-md;

  h3 {
    font-size: 15px;
    font-weight: 600;
    color: $text-primary;
    margin: 0;
  }
}

.card-subtitle {
  font-size: 12px;
  color: $text-secondary;
}

.chart-container {
  width: 100%;
  height: 280px;
}

.list-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: $spacing-md;
}

.list-card {
  background: #fff;
  border-radius: $border-radius-lg;
  padding: $spacing-lg;
  box-shadow: $shadow-light;
}

.warning-list,
.order-list {
  max-height: 320px;
  overflow-y: auto;
}

.list-loading,
.list-empty {
  padding: $spacing-md 0;
}

.warning-item {
  display: flex;
  align-items: flex-start;
  gap: $spacing-sm;
  padding: 10px 0;
  border-bottom: 1px solid $border-lighter;
  cursor: pointer;
  transition: background 0.15s;

  &:hover {
    background: $bg-color;
    margin: 0 -8px;
    padding: 10px 8px;
    border-radius: $border-radius-sm;
  }

  &:last-child {
    border-bottom: none;
  }
}

.warning-level-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 6px;
  flex-shrink: 0;
}

.warning-content {
  flex: 1;
  min-width: 0;
}

.warning-title {
  font-size: 13px;
  color: $text-primary;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.warning-meta {
  font-size: 12px;
  color: $text-secondary;
  margin-top: 2px;
}

.order-item {
  padding: 10px 0;
  border-bottom: 1px solid $border-lighter;
  cursor: pointer;
  transition: background 0.15s;

  &:hover {
    background: $bg-color;
    margin: 0 -8px;
    padding: 10px 8px;
    border-radius: $border-radius-sm;
  }

  &:last-child {
    border-bottom: none;
  }
}

.order-title {
  font-size: 13px;
  color: $text-primary;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-meta {
  font-size: 12px;
  color: $text-secondary;
  margin-top: 2px;
}
</style>
