<template>
  <div class="page-container">
    <!-- 搜索表单 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="预报类型">
          <el-select v-model="query.forecastType" placeholder="全部类型" clearable style="width: 150px">
            <el-option label="趋势预测" value="TREND" />
            <el-option label="风险评估" value="RISK" />
            <el-option label="寿命预测" value="LIFETIME" />
            <el-option label="异常预测" value="ANOMALY" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetch">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
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
      <template #confidence="{ row }">
        <div class="confidence-bar">
          <el-progress
            :percentage="Math.round(row.confidence * 100)"
            :color="getConfidenceColor(row.confidence)"
            :stroke-width="14"
            :text-inside="true"
            style="width: 120px"
          />
        </div>
      </template>
      <template #riskLevel="{ row }">
        <el-tag :type="riskTagType(row.riskLevel)" size="small">{{ row.riskLevel }}</el-tag>
      </template>
      <template #action="{ row }">
        <el-button link type="primary" size="small" @click="handleDetail(row)">查看详情</el-button>
      </template>
    </PageTable>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="预报详情" width="600px">
      <template v-if="currentRow">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="设备名称">{{ currentRow.deviceName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="管网名称">{{ currentRow.pipeName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="预报类型">{{ forecastTypeMap[currentRow.forecastType] || currentRow.forecastType }}</el-descriptions-item>
          <el-descriptions-item label="风险等级">
            <el-tag :type="riskTagType(currentRow.riskLevel)" size="small">{{ currentRow.riskLevel }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="预报时间">{{ currentRow.forecastTime }}</el-descriptions-item>
          <el-descriptions-item label="风险时间">{{ currentRow.riskTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="置信度" :span="2">
            <el-progress
              :percentage="Math.round(currentRow.confidence * 100)"
              :color="getConfidenceColor(currentRow.confidence)"
              :stroke-width="16"
              :text-inside="true"
              style="width: 200px"
            />
          </el-descriptions-item>
          <el-descriptions-item label="趋势描述" :span="2">{{ currentRow.trendDescription || '-' }}</el-descriptions-item>
          <el-descriptions-item label="详细内容" :span="2">{{ currentRow.details || '-' }}</el-descriptions-item>
        </el-descriptions>
        <!-- 趋势图表 -->
        <div class="forecast-chart" ref="chartRef"></div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import * as echarts from 'echarts'
import PageTable from '@/components/common/PageTable.vue'
import { useTable } from '@/composables/useTable'
import { getForecasts } from '@/api/warning'
import type { Forecast } from '@/types/warning.d'

const forecastTypeMap: Record<string, string> = {
  TREND: '趋势预测',
  RISK: '风险评估',
  LIFETIME: '寿命预测',
  ANOMALY: '异常预测',
}

function getConfidenceColor(confidence: number): string {
  const pct = confidence * 100
  if (pct >= 80) return '#43A047'
  if (pct >= 50) return '#FB8C00'
  return '#E53935'
}

function riskTagType(level: string): string {
  const map: Record<string, string> = { '低': 'success', '中': 'warning', '高': 'danger', 'LOW': 'success', 'MEDIUM': 'warning', 'HIGH': 'danger' }
  return map[level] || 'info'
}

// 表格
const { loading, dataList, total, query, fetch, reset, handlePageChange, handleSizeChange } = useTable<Forecast>({
  fetchApi: getForecasts,
  immediate: true,
})

const columns = [
  { prop: 'deviceName', label: '设备', minWidth: 120 },
  { prop: 'pipeName', label: '管网', minWidth: 120 },
  { prop: 'forecastType', label: '预报类型', width: 110 },
  { prop: 'trendDescription', label: '趋势描述', minWidth: 180 },
  { prop: 'riskTime', label: '风险时间', width: 170 },
  { prop: 'confidence', label: '置信度', width: 150, slot: 'confidence' },
  { prop: 'riskLevel', label: '风险等级', width: 100, slot: 'riskLevel' },
  { prop: 'action', label: '操作', width: 100, fixed: 'right' as const, slot: 'action' },
]

// 详情
const detailVisible = ref(false)
const currentRow = ref<Forecast | null>(null)
const chartRef = ref<HTMLElement | null>(null)

function handleDetail(row: Forecast) {
  currentRow.value = row
  detailVisible.value = true
  nextTick(() => {
    renderChart()
  })
}

function renderChart() {
  if (!chartRef.value) return
  const chart = echarts.init(chartRef.value)
  // 模拟趋势数据
  const days = Array.from({ length: 7 }, (_, i) => {
    const d = new Date()
    d.setDate(d.getDate() + i)
    return `${d.getMonth() + 1}/${d.getDate()}`
  })
  const values = days.map(() => Math.round(40 + Math.random() * 40))

  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: days, boundaryGap: false },
    yAxis: { type: 'value', min: 0, max: 100 },
    series: [{
      type: 'line',
      data: values,
      smooth: true,
      areaStyle: { color: 'rgba(30, 136, 229, 0.15)' },
      lineStyle: { color: '#1E88E5', width: 2 },
      itemStyle: { color: '#1E88E5' },
    }],
  })
}
</script>

<style scoped lang="scss">
.page-container {
  padding: $spacing-md;
}

.search-card {
  margin-bottom: $spacing-md;

  :deep(.el-card__body) {
    padding-bottom: 0;
  }
}

.confidence-bar {
  display: flex;
  align-items: center;
}

.forecast-chart {
  width: 100%;
  height: 250px;
  margin-top: $spacing-md;
}
</style>
