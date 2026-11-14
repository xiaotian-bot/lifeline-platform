<template>
  <div class="gis-map-page">
    <!-- 顶部工具栏 -->
    <div class="map-toolbar">
      <div class="toolbar-left">
        <h3 class="toolbar-title">管网GIS地图</h3>
      </div>
      <div class="toolbar-right">
        <el-select v-model="currentPipeType" placeholder="全部管网类型" clearable style="width: 160px" @change="loadGeoJson">
          <el-option v-for="item in pipeTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button :icon="Refresh" :loading="mapLoading" @click="loadGeoJson">刷新</el-button>
        <el-button :icon="FullScreen" @click="toggleFullscreen">全屏</el-button>
      </div>
    </div>

    <!-- 地图容器 -->
    <div class="map-wrapper" v-loading="mapLoading">
      <div ref="mapRef" class="main-map"></div>

      <!-- 图例 -->
      <div class="map-legend">
        <div class="legend-title">管网类型</div>
        <div v-for="item in pipeTypeOptions" :key="item.value" class="legend-item">
          <span class="legend-line" :style="{ backgroundColor: pipeTypeColorMap[item.value] }"></span>
          <span class="legend-label">{{ item.label }}</span>
        </div>
        <div class="legend-divider"></div>
        <div class="legend-title">设备</div>
        <div class="legend-item">
          <span class="legend-dot" style="background: #43A047"></span>
          <span class="legend-label">在线</span>
        </div>
        <div class="legend-item">
          <span class="legend-dot" style="background: #909399"></span>
          <span class="legend-label">离线</span>
        </div>
        <div class="legend-item">
          <span class="legend-dot" style="background: #E53935"></span>
          <span class="legend-label">故障</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh, FullScreen } from '@element-plus/icons-vue'
import { getPipeGeoJson, getDevices } from '@/api/pipe'
import type { GeoJsonFeatureCollection, MonitorDevice } from '@/types/pipe.d'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

const router = useRouter()

const pipeTypeOptions = [
  { label: '燃气', value: 'gas' },
  { label: '给水', value: 'water' },
  { label: '排水', value: 'drainage' },
  { label: '热力', value: 'heating' },
  { label: '雨水', value: 'storm' },
]

const pipeTypeColorMap: Record<string, string> = {
  gas: '#FB8C00',
  water: '#1E88E5',
  drainage: '#43A047',
  heating: '#E53935',
  storm: '#7E57C2',
}

const pipeTypeLabelMap: Record<string, string> = {
  gas: '燃气', water: '给水', drainage: '排水', heating: '热力', storm: '雨水',
}

const deviceStatusColor: Record<number, string> = {
  1: '#43A047',   // 在线
  0: '#909399',   // 离线
  2: '#E53935',   // 故障
}

const deviceStatusLabel: Record<number, string> = {
  1: '在线',
  0: '离线',
  2: '故障',
}

const mapRef = ref<HTMLElement>()
const mapLoading = ref(false)
const currentPipeType = ref<string>('')

let map: L.Map | null = null
let pipeLayer: L.LayerGroup | null = null
let deviceLayer: L.LayerGroup | null = null

async function initMap() {
  if (!mapRef.value) return

  map = L.map(mapRef.value).setView([36.8, 109.5], 13)

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap contributors',
    maxZoom: 19,
  }).addTo(map)

  pipeLayer = L.layerGroup().addTo(map)
  deviceLayer = L.layerGroup().addTo(map)

  await loadGeoJson()
  await loadDevices()
}

async function loadGeoJson() {
  if (!map || !pipeLayer) return
  mapLoading.value = true

  try {
    const geoData = await getPipeGeoJson(currentPipeType.value || undefined)
    pipeLayer.clearLayers()

    if (!geoData?.features?.length) return

    const geoJsonLayer = L.geoJSON(geoData as GeoJSON.GeoJsonObject, {
      style: (feature) => {
        const props = feature?.properties as Record<string, unknown> || {}
        const pType = (props.pipeType as string) || ''
        return {
          color: pipeTypeColorMap[pType] || '#1E88E5',
          weight: 4,
          opacity: 0.8,
        }
      },
      onEachFeature: (feature, layer) => {
        const props = feature.properties as Record<string, unknown> || {}
        const pipeId = props.pipeId as number
        const pipeCode = (props.pipeCode as string) || ''
        const pType = (props.pipeType as string) || ''
        const diameter = props.diameter as number || 0
        const material = (props.material as string) || ''
        layer.bindPopup(buildPopup(pipeCode, pType, diameter, material, pipeId))
      },
    })

    pipeLayer.addLayer(geoJsonLayer)

    // 自适应范围
    const bounds = geoJsonLayer.getBounds()
    if (bounds.isValid()) {
      map.fitBounds(bounds, { padding: [40, 40] })
    }
  } catch {
    // handled
  } finally {
    mapLoading.value = false
  }
}

function buildPopup(pipeCode: string, pType: string, diameter: number, material: string, pipeId: number): string {
  return `
    <div style="min-width:200px">
      <div style="font-weight:600;margin-bottom:6px;font-size:14px">${pipeCode}</div>
      <div>类型：${pipeTypeLabelMap[pType] || pType}</div>
      <div>管径：${diameter}mm</div>
      <div>材质：${material}</div>
      <div style="margin-top:8px">
        <a href="javascript:void(0)" onclick="window.__goPipeDetail__(${pipeId})" style="color:#1E88E5;text-decoration:underline">查看详情</a>
      </div>
    </div>
  `
}

async function loadDevices() {
  if (!map || !deviceLayer) return

  try {
    const res = await getDevices({ page: 1, size: 500 })
    deviceLayer.clearLayers()

    const devices = res?.records || []
    devices.forEach((device: MonitorDevice) => {
      if (!device.lng || !device.lat) return

      const color = deviceStatusColor[device.status] || '#909399'
      const marker = L.circleMarker([device.lat, device.lng], {
        radius: 6,
        fillColor: color,
        color: '#fff',
        weight: 2,
        fillOpacity: 0.9,
      })

      marker.bindPopup(`
        <div style="min-width:160px">
          <div style="font-weight:600;margin-bottom:4px">${device.deviceName}</div>
          <div>编号：${device.deviceCode}</div>
          <div>类型：${device.deviceType}</div>
          <div>状态：${deviceStatusLabel[device.status] || '未知'}</div>
        </div>
      `)

      deviceLayer!.addLayer(marker)
    })
  } catch {
    // handled
  }
}

function toggleFullscreen() {
  const el = document.querySelector('.gis-map-page') as HTMLElement
  if (!el) return
  if (!document.fullscreenElement) {
    el.requestFullscreen()
  } else {
    document.exitFullscreen()
  }
}

// 全局跳转函数（popup 中使用）
onMounted(() => {
  ;(window as unknown as Record<string, unknown>).__goPipeDetail__ = (pipeId: number) => {
    router.push(`/asset/pipe/${pipeId}`)
  }
  nextTick(initMap)
})

onBeforeUnmount(() => {
  if (map) {
    map.remove()
    map = null
  }
  delete (window as unknown as Record<string, unknown>).__goPipeDetail__
})
</script>

<style scoped lang="scss">
.gis-map-page {
  width: 100%;
  height: calc(100vh - $header-height - $tags-view-height - 48px);
  display: flex;
  flex-direction: column;
  position: relative;
}

.map-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $spacing-sm $spacing-md;
  background: #fff;
  border-bottom: 1px solid $border-light;
  z-index: 10;
}

.toolbar-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: $text-primary;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
}

.map-wrapper {
  flex: 1;
  position: relative;
}

.main-map {
  width: 100%;
  height: 100%;
  z-index: 1;
}

.map-legend {
  position: absolute;
  bottom: 24px;
  right: 24px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: $border-radius-lg;
  padding: 12px 16px;
  box-shadow: $shadow-base;
  z-index: 1000;
  min-width: 120px;
}

.legend-title {
  font-size: 12px;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 6px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.legend-line {
  display: inline-block;
  width: 20px;
  height: 3px;
  border-radius: 2px;
}

.legend-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 1.5px solid #fff;
  box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.1);
}

.legend-label {
  font-size: 12px;
  color: $text-regular;
}

.legend-divider {
  border-top: 1px solid $border-lighter;
  margin: 8px 0;
}
</style>
