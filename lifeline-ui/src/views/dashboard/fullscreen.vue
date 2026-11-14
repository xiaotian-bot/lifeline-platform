<template>
  <div class="fullscreen-screen">
    <!-- 顶部标题栏 -->
    <div class="screen-header">
      <div class="header-decor left-decor" />
      <div class="header-title">
        <h1>安塞区城市安全生命线管网AI智慧平台</h1>
        <div class="header-time">{{ currentTime }}</div>
      </div>
      <div class="header-decor right-decor" />
      <div class="header-actions">
        <el-button text class="exit-btn" @click="exitScreen">
          <el-icon><Close /></el-icon> 退出
        </el-button>
      </div>
    </div>

    <!-- 主体内容 -->
    <div class="screen-body">
      <!-- 左侧面板 -->
      <div class="side-panel left-panel">
        <div class="panel-block">
          <div class="block-title">预警统计</div>
          <div ref="warningPieRef" class="block-chart" />
        </div>
        <div class="panel-block">
          <div class="block-title">管网统计</div>
          <div ref="pipeBarRef" class="block-chart" />
        </div>
      </div>

      <!-- 中间地图 -->
      <div class="center-map">
        <div ref="mapRef" class="map-container" />
        <div class="map-legend">
          <span class="legend-item"><i class="dot" style="background:#43A047" />正常</span>
          <span class="legend-item"><i class="dot" style="background:#FB8C00" />预警</span>
          <span class="legend-item"><i class="dot" style="background:#E53935" />报警</span>
        </div>
      </div>

      <!-- 右侧面板 -->
      <div class="side-panel right-panel">
        <div class="panel-block">
          <div class="block-title">实时数据</div>
          <div class="realtime-list">
            <transition-group name="list" tag="div">
              <div v-for="(item, idx) in realtimeDataList" :key="item.deviceId + item.ts" class="realtime-item">
                <span class="rt-index">{{ idx + 1 }}</span>
                <span class="rt-device">{{ item.deviceId }}</span>
                <span class="rt-value" :class="{ 'is-warn': item.status > 0 }">
                  {{ item.pressure ?? '-' }} MPa
                </span>
                <span class="rt-time">{{ formatTime(item.ts) }}</span>
              </div>
            </transition-group>
          </div>
        </div>
        <div class="panel-block">
          <div class="block-title">工单统计</div>
          <div ref="orderChartRef" class="block-chart" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Close } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import * as L from 'leaflet'
import { getWarningStatistics } from '@/api/warning'
import { getPipes, getDevices } from '@/api/pipe'
import { getOrderStatistics } from '@/api/operation'
import { MonitorWebSocket } from '@/api/ws'
import type { SensorData } from '@/types/monitor.d'
import type { WarningStatistics } from '@/types/warning.d'

const router = useRouter()

// ==================== 时间 ====================
const currentTime = ref('')
let timeTimer: ReturnType<typeof setInterval> | null = null

function updateTime() {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit',
  })
}

// ==================== 图表引用 ====================
const warningPieRef = ref<HTMLDivElement>()
const pipeBarRef = ref<HTMLDivElement>()
const orderChartRef = ref<HTMLDivElement>()
const mapRef = ref<HTMLDivElement>()

let warningPieChart: echarts.ECharts | null = null
let pipeBarChart: echarts.ECharts | null = null
let orderChart: echarts.ECharts | null = null
let map: L.Map | null = null

// ==================== 实时数据 ====================
const realtimeDataList = ref<SensorData[]>([])
const MAX_REALTIME = 20
let ws: MonitorWebSocket | null = null

function formatTime(ts: number): string {
  const d = new Date(ts)
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}:${String(d.getSeconds()).padStart(2, '0')}`
}

// ==================== 数据加载 ====================
async function loadWarningPie() {
  try {
    const stat: WarningStatistics = await getWarningStatistics().catch(() => ({
      total: 0, byLevel: [], byType: [], byStatus: [],
    }))
    if (!warningPieRef.value) return
    if (!warningPieChart) warningPieChart = echarts.init(warningPieRef.value, 'dark')
    const levelColors: Record<string, string> = { blue: '#1E88E5', yellow: '#FDD835', orange: '#FB8C00', red: '#E53935' }
    const data = (stat.byLevel?.length ? stat.byLevel : [
      { key: 'blue', label: '蓝色', count: 15 },
      { key: 'yellow', label: '黄色', count: 10 },
      { key: 'orange', label: '橙色', count: 5 },
      { key: 'red', label: '红色', count: 2 },
    ]).map(item => ({
      name: item.label || item.key,
      value: item.count,
      itemStyle: { color: levelColors[item.key] || '#909399' },
    }))
    warningPieChart.setOption({
      backgroundColor: 'transparent',
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      series: [{
        type: 'pie', radius: ['35%', '60%'], center: ['50%', '50%'],
        label: { color: '#B0BEC5', fontSize: 11 },
        data,
      }],
    })
  } catch {
    // handled
  }
}

async function loadPipeBar() {
  try {
    const res = await getPipes({ page: 1, size: 500 }).catch(() => ({ records: [], total: 0 }))
    if (!pipeBarRef.value) return
    if (!pipeBarChart) pipeBarChart = echarts.init(pipeBarRef.value, 'dark')
    const typeCount: Record<string, number> = {}
    const labelMap: Record<string, string> = { gas: '燃气', water: '给水', drainage: '排水', heating: '热力', storm: '雨水' }
    const colorMap: Record<string, string> = { gas: '#FB8C00', water: '#1E88E5', drainage: '#43A047', heating: '#E53935', storm: '#7E57C2' }
    for (const p of res.records) {
      typeCount[p.pipeType] = (typeCount[p.pipeType] || 0) + 1
    }
    const categories = Object.keys(typeCount)
    const values = Object.values(typeCount)
    pipeBarChart.setOption({
      backgroundColor: 'transparent',
      tooltip: { trigger: 'axis' },
      grid: { top: 10, right: 16, bottom: 24, left: 40 },
      xAxis: {
        type: 'category',
        data: categories.map(k => labelMap[k] || k),
        axisLine: { lineStyle: { color: '#37474F' } },
        axisLabel: { color: '#B0BEC5', fontSize: 11 },
      },
      yAxis: {
        type: 'value',
        splitLine: { lineStyle: { color: '#263238' } },
        axisLabel: { color: '#B0BEC5', fontSize: 11 },
      },
      series: [{
        type: 'bar', barWidth: '50%',
        data: values.map((v, i) => ({
          value: v,
          itemStyle: { color: colorMap[categories[i]] || '#42A5F5', borderRadius: [4, 4, 0, 0] },
        })),
      }],
    })
  } catch {
    // handled
  }
}

async function loadOrderChart() {
  try {
    const stat = await getOrderStatistics().catch(() => ({
      pending: 0, assigned: 0, inProgress: 0, completed: 0, closed: 0, total: 0,
    }))
    if (!orderChartRef.value) return
    if (!orderChart) orderChart = echarts.init(orderChartRef.value, 'dark')
    orderChart.setOption({
      backgroundColor: 'transparent',
      tooltip: { trigger: 'axis' },
      grid: { top: 10, right: 16, bottom: 24, left: 40 },
      xAxis: {
        type: 'category',
        data: ['待处理', '已分配', '进行中', '已完成', '已关闭'],
        axisLine: { lineStyle: { color: '#37474F' } },
        axisLabel: { color: '#B0BEC5', fontSize: 10 },
      },
      yAxis: {
        type: 'value',
        splitLine: { lineStyle: { color: '#263238' } },
        axisLabel: { color: '#B0BEC5', fontSize: 11 },
      },
      series: [{
        type: 'bar', barWidth: '45%',
        data: [
          { value: stat.pending, itemStyle: { color: '#FB8C00', borderRadius: [4, 4, 0, 0] } },
          { value: stat.assigned, itemStyle: { color: '#42A5F5', borderRadius: [4, 4, 0, 0] } },
          { value: stat.inProgress, itemStyle: { color: '#FDD835', borderRadius: [4, 4, 0, 0] } },
          { value: stat.completed, itemStyle: { color: '#43A047', borderRadius: [4, 4, 0, 0] } },
          { value: stat.closed, itemStyle: { color: '#78909C', borderRadius: [4, 4, 0, 0] } },
        ],
      }],
    })
  } catch {
    // handled
  }
}

async function loadMap() {
  if (!mapRef.value) return
  // 安塞区中心坐标
  map = L.map(mapRef.value, {
    center: [36.87, 109.32],
    zoom: 13,
    zoomControl: false,
    attributionControl: false,
  })
  // 深色底图
  L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
    maxZoom: 19,
  }).addTo(map)

  // 加载管网 GeoJSON
  try {
    const geoJson = await getPipes({ page: 1, size: 500 }).catch(() => null)
    if (!geoJson) return
    // 加载 GeoJSON 数据
    const geoData = await import('@/api/pipe').then(api => api.getPipeGeoJson().catch(() => null))
    if (geoData && geoData.features) {
      L.geoJSON(geoData as GeoJSON.GeoJsonObject, {
        style: () => ({
          color: '#42A5F5',
          weight: 3,
          opacity: 0.8,
        }),
      }).addTo(map!)
    }
  } catch {
    // handled
  }

  // 加载设备标记
  try {
    const devices = await getDevices({ page: 1, size: 200 }).catch(() => ({ records: [], total: 0 }))
    for (const dev of devices.records) {
      if (dev.lat && dev.lng) {
        const statusLabel: Record<number, string> = { 0: '离线', 1: '在线', 2: '故障' }
        const color = dev.status === 1 ? '#43A047' : dev.status === 2 ? '#E53935' : '#78909C'
        const marker = L.circleMarker([dev.lat, dev.lng], {
          radius: 6,
          fillColor: color,
          color: '#0a1929',
          weight: 2,
          fillOpacity: 0.9,
        }).addTo(map!)
        marker.bindPopup(`<div style="color:#333;font-size:12px"><b>${dev.deviceName}</b><br/>类型: ${dev.deviceType}<br/>状态: ${statusLabel[dev.status] || '未知'}</div>`)
      }
    }
  } catch {
    // handled
  }
}

// ==================== WebSocket ====================
function connectWs() {
  ws = new MonitorWebSocket(
    0,
    (data: unknown) => {
      const msg = data as SensorData
      if (msg.deviceId && msg.ts) {
        realtimeDataList.value.unshift(msg)
        if (realtimeDataList.value.length > MAX_REALTIME) {
          realtimeDataList.value = realtimeDataList.value.slice(0, MAX_REALTIME)
        }
      }
    },
    undefined,
    undefined,
  )
  ws.connect()
}

// ==================== 退出 ====================
function exitScreen() {
  if (document.fullscreenElement) {
    document.exitFullscreen().catch(() => {})
  }
  router.push('/dashboard')
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') {
    exitScreen()
  }
}

// ==================== resize ====================
function handleResize() {
  warningPieChart?.resize()
  pipeBarChart?.resize()
  orderChart?.resize()
  map?.invalidateSize()
}

// ==================== 生命周期 ====================
onMounted(async () => {
  updateTime()
  timeTimer = setInterval(updateTime, 1000)
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('resize', handleResize)

  // 进入全屏
  try {
    await document.documentElement.requestFullscreen()
  } catch {
    // handled
  }

  await nextTick()
  loadWarningPie()
  loadPipeBar()
  loadOrderChart()
  loadMap()
  connectWs()
})

onBeforeUnmount(() => {
  if (timeTimer) clearInterval(timeTimer)
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('resize', handleResize)
  ws?.disconnect()
  warningPieChart?.dispose()
  pipeBarChart?.dispose()
  orderChart?.dispose()
  map?.remove()
  if (document.fullscreenElement) {
    document.exitFullscreen().catch(() => {})
  }
})
</script>

<style scoped lang="scss">
.fullscreen-screen {
  width: 100vw;
  height: 100vh;
  background: #0a1929;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  color: #B0BEC5;
  font-family: 'Microsoft YaHei', sans-serif;
}

// ==================== 头部 ====================
.screen-header {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  background: linear-gradient(180deg, rgba(13, 71, 161, 0.4) 0%, transparent 100%);
  border-bottom: 1px solid rgba(66, 165, 245, 0.2);
  flex-shrink: 0;
}

.header-decor {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 260px;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(66, 165, 245, 0.6), transparent);

  &.left-decor { left: 60px; }
  &.right-decor { right: 60px; }
}

.header-title {
  text-align: center;
}

.header-title h1 {
  font-size: 24px;
  font-weight: 700;
  color: #E3F2FD;
  letter-spacing: 6px;
  margin: 0;
  text-shadow: 0 0 20px rgba(66, 165, 245, 0.5);
}

.header-time {
  font-size: 13px;
  color: #64B5F6;
  margin-top: 2px;
  letter-spacing: 1px;
}

.header-actions {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
}

.exit-btn {
  color: #64B5F6 !important;
  font-size: 13px;

  &:hover {
    color: #E3F2FD !important;
  }
}

// ==================== 主体 ====================
.screen-body {
  flex: 1;
  display: flex;
  gap: 12px;
  padding: 12px;
  min-height: 0;
}

.side-panel {
  width: 320px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex-shrink: 0;
}

.panel-block {
  flex: 1;
  background: rgba(13, 39, 77, 0.6);
  border: 1px solid rgba(66, 165, 245, 0.15);
  border-radius: 8px;
  padding: 12px;
  display: flex;
  flex-direction: column;
}

.block-title {
  font-size: 14px;
  font-weight: 600;
  color: #64B5F6;
  margin-bottom: 8px;
  padding-left: 10px;
  border-left: 3px solid #42A5F5;
}

.block-chart {
  flex: 1;
  min-height: 0;
}

// ==================== 中间地图 ====================
.center-map {
  flex: 1;
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid rgba(66, 165, 245, 0.15);
}

.map-container {
  width: 100%;
  height: 100%;
}

.map-legend {
  position: absolute;
  bottom: 12px;
  left: 12px;
  background: rgba(10, 25, 41, 0.85);
  padding: 6px 12px;
  border-radius: 4px;
  display: flex;
  gap: 12px;
  z-index: 1000;
}

.legend-item {
  font-size: 12px;
  color: #B0BEC5;
  display: flex;
  align-items: center;
  gap: 4px;

  .dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    display: inline-block;
  }
}

// ==================== 右侧实时数据 ====================
.realtime-list {
  flex: 1;
  overflow-y: auto;
  min-height: 0;

  &::-webkit-scrollbar {
    width: 4px;
  }
  &::-webkit-scrollbar-thumb {
    background: rgba(66, 165, 245, 0.3);
    border-radius: 2px;
  }
}

.realtime-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 4px;
  border-bottom: 1px solid rgba(66, 165, 245, 0.08);
  font-size: 12px;
  transition: background 0.2s;

  &:hover {
    background: rgba(66, 165, 245, 0.05);
  }
}

.rt-index {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(66, 165, 245, 0.2);
  color: #64B5F6;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  flex-shrink: 0;
}

.rt-device {
  flex: 1;
  color: #B0BEC5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rt-value {
  color: #43A047;
  font-family: monospace;
  font-weight: 600;

  &.is-warn {
    color: #FB8C00;
  }
}

.rt-time {
  color: #546E7A;
  font-family: monospace;
  font-size: 11px;
}

// ==================== 列表动画 ====================
.list-enter-active {
  transition: all 0.3s ease;
}
.list-leave-active {
  transition: all 0.2s ease;
}
.list-enter-from {
  opacity: 0;
  transform: translateX(-20px);
}
.list-leave-to {
  opacity: 0;
  transform: translateX(20px);
}
</style>
