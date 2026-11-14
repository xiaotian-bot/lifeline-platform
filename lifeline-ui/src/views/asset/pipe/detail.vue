<template>
  <div class="pipe-detail-page" v-loading="pageLoading">
    <!-- 返回按钮 -->
    <div class="page-header">
      <el-button :icon="ArrowLeft" @click="router.back()">返回列表</el-button>
      <h3 class="page-title">管网详情</h3>
    </div>

    <template v-if="pipeData">
      <!-- 基本信息 -->
      <el-card class="info-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>基本信息</span>
            <el-tag :color="pipeTypeColorMap[pipeData.pipeType]" effect="dark" size="small" style="border: none; color: #fff">
              {{ pipeTypeLabelMap[pipeData.pipeType] || pipeData.pipeType }}
            </el-tag>
          </div>
        </template>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="管网编号">{{ pipeData.pipeCode }}</el-descriptions-item>
          <el-descriptions-item label="管网名称">{{ pipeData.pipeName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="管网类型">{{ pipeTypeLabelMap[pipeData.pipeType] || pipeData.pipeType }}</el-descriptions-item>
          <el-descriptions-item label="管径(mm)">{{ pipeData.diameter }}</el-descriptions-item>
          <el-descriptions-item label="材质">{{ pipeData.material }}</el-descriptions-item>
          <el-descriptions-item label="长度(m)">{{ pipeData.length }}</el-descriptions-item>
          <el-descriptions-item label="埋深(m)">{{ pipeData.depth }}</el-descriptions-item>
          <el-descriptions-item label="压力等级">{{ pipeData.pressureLevel || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <StatusTag :status="statusMap[pipeData.status] || 'normal'" :label="pipeData.status" />
          </el-descriptions-item>
          <el-descriptions-item label="安装日期">{{ pipeData.installDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="最近检测">{{ pipeData.lastInspectDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="起点坐标">{{ pipeData.startLng?.toFixed(6) }}, {{ pipeData.startLat?.toFixed(6) }}</el-descriptions-item>
          <el-descriptions-item label="终点坐标">{{ pipeData.endLng?.toFixed(6) }}, {{ pipeData.endLat?.toFixed(6) }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- GIS 地图 -->
      <el-card class="map-card" shadow="never">
        <template #header><span>空间位置</span></template>
        <div ref="mapRef" class="detail-map"></div>
      </el-card>

      <!-- 关联设备 -->
      <el-card class="devices-card" shadow="never">
        <template #header><span>关联设备 ({{ archiveData?.devices?.length || 0 }})</span></template>
        <el-table :data="archiveData?.devices || []" stripe border v-loading="devicesLoading">
          <el-table-column prop="deviceCode" label="设备编号" width="140" />
          <el-table-column prop="deviceName" label="设备名称" min-width="140" />
          <el-table-column prop="deviceType" label="设备类型" width="120">
            <template #default="{ row }">
              <el-tag size="small">{{ deviceTypeLabelMap[row.deviceType] || row.deviceType }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="90" align="center">
            <template #default="{ row }">
              <StatusTag :status="deviceStatusMap[row.status] ?? 'offline'" :label="deviceStatusLabel[row.status] ?? String(row.status)" />
            </template>
          </el-table-column>
          <el-table-column prop="lastOnlineTime" label="最近上线" width="170" />
          <template #empty>
            <el-empty description="暂无关联设备" :image-size="60" />
          </template>
        </el-table>
      </el-card>

      <!-- 关联井室 -->
      <el-card class="manholes-c" shadow="never">
        <template #header><span>关联井室 ({{ archiveData?.manholes?.length || 0 }})</span></template>
        <el-table :data="archiveData?.manholes || []" stripe border v-loading="manholesLoading">
          <el-table-column prop="manholeCode" label="编号" width="140" />
          <el-table-column prop="manholeName" label="名称" min-width="140" />
          <el-table-column prop="manholeType" label="类型" width="100" />
          <el-table-column prop="diameter" label="管径(mm)" width="100" align="right" />
          <el-table-column prop="depth" label="深度(m)" width="90" align="right" />
          <template #empty>
            <el-empty description="暂无关联井室" :image-size="60" />
          </template>
        </el-table>
      </el-card>

      <!-- 一管一档 -->
      <el-card class="archive-card" shadow="never" v-if="archiveData">
        <template #header><span>一管一档</span></template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="管网编号">{{ pipeData.pipeCode }}</el-descriptions-item>
          <el-descriptions-item label="管网名称">{{ pipeData.pipeName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="管网类型">{{ pipeTypeLabelMap[pipeData.pipeType] }}</el-descriptions-item>
          <el-descriptions-item label="关联设备数">{{ archiveData.devices?.length || 0 }} 台</el-descriptions-item>
          <el-descriptions-item label="关联井室数">{{ archiveData.manholes?.length || 0 }} 座</el-descriptions-item>
          <el-descriptions-item label="上游管网数">{{ archiveData.topology?.upstreamPipes?.length || 0 }} 段</el-descriptions-item>
          <el-descriptions-item label="下游管网数">{{ archiveData.topology?.downstreamPipes?.length || 0 }} 段</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ pipeData.createTime || '-' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 扩展属性 -->
        <div v-if="pipeData.attributes && Object.keys(pipeData.attributes).length" class="ext-attrs">
          <h4 class="sub-title">扩展属性</h4>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item v-for="(val, key) in pipeData.attributes" :key="key" :label="String(key)">
              {{ val }}
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </el-card>
    </template>

    <el-empty v-if="!pageLoading && !pipeData" description="管网数据不存在" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getPipeById, getPipeArchive } from '@/api/pipe'
import type { PipeSegment, PipeArchive } from '@/types/pipe.d'
import StatusTag from '@/components/common/StatusTag.vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

const route = useRoute()
const router = useRouter()

const pipeId = Number(route.params.id)

const pipeData = ref<PipeSegment | null>(null)
const archiveData = ref<PipeArchive | null>(null)
const pageLoading = ref(true)
const devicesLoading = ref(false)
const manholesLoading = ref(false)

const mapRef = ref<HTMLElement>()

const pipeTypeColorMap: Record<string, string> = {
  gas: '#FB8C00',
  water: '#1E88E5',
  drainage: '#43A047',
  heating: '#E53935',
  storm: '#7E57C2',
}

const pipeTypeLabelMap: Record<string, string> = {
  gas: '燃气',
  water: '给水',
  drainage: '排水',
  heating: '热力',
  storm: '雨水',
}

const statusMap: Record<string, string> = {
  '正常': 'normal',
  '运行': 'normal',
  '预警': 'warning',
  '故障': 'fault',
  '废弃': 'offline',
}

const deviceTypeLabelMap: Record<string, string> = {
  pressure: '压力',
  temp: '温度',
  flow: '流量',
  vibration: '振动',
  corrosion: '腐蚀',
  leakage: '泄漏',
  video: '视频',
}

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

async function loadData() {
  pageLoading.value = true
  try {
    const [pipe, archive] = await Promise.all([
      getPipeById(pipeId),
      getPipeArchive(pipeId).catch(() => null),
    ])
    pipeData.value = pipe
    archiveData.value = archive

    await nextTick()
    initMap(pipe)
  } catch {
    // handled by interceptor
  } finally {
    pageLoading.value = false
  }
}

function initMap(pipe: PipeSegment) {
  if (!mapRef.value || !pipe.startLng || !pipe.startLat) return

  const map = L.map(mapRef.value).setView(
    [pipe.startLat, pipe.startLng],
    15
  )

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap contributors',
  }).addTo(map)

  // 绘制管线
  if (pipe.endLng && pipe.endLat) {
    const line = L.polyline(
      [
        [pipe.startLat, pipe.startLng],
        [pipe.endLat, pipe.endLng],
      ],
      {
        color: pipeTypeColorMap[pipe.pipeType] || '#1E88E5',
        weight: 5,
        opacity: 0.8,
      }
    ).addTo(map)

    line.bindPopup(`
      <div style="min-width:180px">
        <strong>${pipe.pipeCode}</strong><br/>
        类型：${pipeTypeLabelMap[pipe.pipeType] || pipe.pipeType}<br/>
        管径：${pipe.diameter}mm<br/>
        长度：${pipe.length}m
      </div>
    `)

    map.fitBounds(line.getBounds(), { padding: [30, 30] })
  }

  // 起点标记
  L.marker([pipe.startLat, pipe.startLng]).addTo(map).bindPopup('起点')
  // 终点标记
  if (pipe.endLng && pipe.endLat) {
    L.marker([pipe.endLat, pipe.endLng]).addTo(map).bindPopup('终点')
  }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.pipe-detail-page {
  padding: $spacing-md;
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.page-header {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  margin-bottom: $spacing-sm;

  .page-title {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
    color: $text-primary;
  }
}

.info-card,
.map-card,
.devices-card,
.manholes-c,
.archive-card {
  :deep(.el-card__header) {
    padding: $spacing-md;
    font-weight: 600;
  }
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.detail-map {
  width: 100%;
  height: 350px;
  border-radius: $border-radius-base;
  z-index: 0;
}

.sub-title {
  margin: $spacing-md 0 $spacing-sm;
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
}

.ext-attrs {
  margin-top: $spacing-md;
}
</style>
