<template>
  <div class="page-container">
    <el-card shadow="never" class="form-card">
      <template #header>
        <div class="card-header">
          <span class="card-header__title">趋势预报</span>
          <el-tag type="info" size="small">AI智能预测</el-tag>
        </div>
      </template>
      <el-form :model="form" label-width="100px" class="forecast-form">
        <el-row :gutter="24">
          <el-col :span="6">
            <el-form-item label="设备编码">
              <el-input v-model="form.deviceCode" placeholder="如 DEV-GAS-001" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="管网类型">
              <el-select v-model="form.pipeType" placeholder="选择管网类型" clearable style="width: 100%">
                <el-option label="燃气" value="gas" />
                <el-option label="供水" value="water" />
                <el-option label="排水" value="drainage" />
                <el-option label="供热" value="heating" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="指标类型">
              <el-select v-model="form.metricType" placeholder="选择指标" clearable style="width: 100%">
                <el-option label="压力" value="pressure" />
                <el-option label="温度" value="temperature" />
                <el-option label="流量" value="flow" />
                <el-option label="振动" value="vibration" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="预报天数">
              <el-input-number v-model="form.horizonDays" :min="1" :max="30" :step="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="历史数据">
          <el-input
            v-model="seriesInput"
            type="textarea"
            :rows="3"
            placeholder="输入历史时序数值，逗号分隔或JSON数组，如: 1.2, 1.5, 1.3, 1.4, 1.6, 1.8, 2.0, 1.9"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleForecast">开始预报</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="info" plain @click="handleFillDemo">填入示例数据</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 预报结果 -->
    <template v-if="result !== undefined">
      <template v-if="result === null">
        <el-card shadow="never" class="result-card degraded-card">
          <el-empty description="AI分析暂不可用，请稍后重试" :image-size="80" />
        </el-card>
      </template>
      <template v-else>
        <!-- 概览卡片 -->
        <el-row :gutter="16" class="overview-row">
          <el-col :span="6">
            <el-card shadow="never" class="overview-card">
              <div class="overview-card__label">预报类型</div>
              <div class="overview-card__value">{{ forecastTypeLabel(result.forecastType) }}</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="never" class="overview-card">
              <div class="overview-card__label">置信度</div>
              <div class="overview-card__value">{{ result.confidence != null ? (result.confidence * 100).toFixed(1) + '%' : '-' }}</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="never" class="overview-card">
              <div class="overview-card__label">风险时间</div>
              <div class="overview-card__value">{{ result.riskTime || '暂无风险' }}</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="never" class="overview-card">
              <div class="overview-card__label">趋势描述</div>
              <div class="overview-card__value text-ellipsis" :title="result.trendDesc || ''">{{ result.trendDesc || '-' }}</div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 趋势图表 -->
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="card-header__title">趋势预报图表</span>
            </div>
          </template>
          <div ref="chartRef" class="chart-container" />
        </el-card>

        <!-- 预报详情 -->
        <el-card v-if="result.reportContent" shadow="never" class="report-card">
          <template #header>
            <div class="card-header">
              <span class="card-header__title">预报报告</span>
            </div>
          </template>
          <div class="report-content">{{ result.reportContent }}</div>
        </el-card>
      </template>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, nextTick, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { forecast } from '@/api/ai'
import type { ForecastResult } from '@/types/ai.d'

const form = reactive({
  deviceCode: '',
  pipeType: '',
  metricType: '',
  horizonDays: 7,
})
const seriesInput = ref('')
const loading = ref(false)
const result = ref<ForecastResult | null | undefined>(undefined)
const chartRef = ref<HTMLDivElement | null>(null)
let chartInstance: echarts.ECharts | null = null

const forecastTypeMap: Record<string, string> = {
  TREND: '趋势预报',
  RISK: '风险预警',
  STABLE: '稳定运行',
}
function forecastTypeLabel(type: string | null) {
  if (!type) return '-'
  return forecastTypeMap[type] || type
}

function parseSeries(input: string): number[] | null {
  const trimmed = input.trim()
  if (!trimmed) return null
  try {
    if (trimmed.startsWith('[')) {
      const arr = JSON.parse(trimmed)
      if (Array.isArray(arr)) return arr.map(Number).filter(n => !isNaN(n))
    }
    return trimmed.split(/[,\s]+/).map(Number).filter(n => !isNaN(n))
  } catch {
    return null
  }
}

function renderChart(data: ForecastResult, historySeries: number[]) {
  if (!chartRef.value) return
  if (chartInstance) chartInstance.dispose()
  chartInstance = echarts.init(chartRef.value)

  const historyLabels = historySeries.map((_, i) => `T-${historySeries.length - i}`)
  const forecastPoints = data.forecastSeries || []
  const forecastLabels = forecastPoints.map(p => p.time?.substring(5, 10) || '')
  const forecastValues = forecastPoints.map(p => p.value)

  const allLabels = [...historyLabels, ...forecastLabels]
  const historyData = [...historySeries, ...forecastLabels.map(() => null)]
  const forecastData = [...historySeries.map((_, i) => i === historySeries.length - 1 ? historySeries[historySeries.length - 1] : null), ...forecastValues]

  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderColor: '#E4E7ED',
      textStyle: { color: '#303133', fontSize: 13 },
    },
    legend: {
      data: ['历史数据', '预报数据'],
      top: 0,
      textStyle: { color: '#606266' },
    },
    grid: { left: 50, right: 30, top: 40, bottom: 30 },
    xAxis: {
      type: 'category',
      data: allLabels,
      axisLabel: { color: '#909399', fontSize: 11 },
      axisLine: { lineStyle: { color: '#E4E7ED' } },
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#909399' },
      splitLine: { lineStyle: { color: '#F0F2F5' } },
    },
    series: [
      {
        name: '历史数据',
        type: 'line',
        data: historyData,
        smooth: true,
        lineStyle: { color: '#1E88E5', width: 2 },
        itemStyle: { color: '#1E88E5' },
        areaStyle: { color: 'rgba(30,136,229,0.08)' },
      },
      {
        name: '预报数据',
        type: 'line',
        data: forecastData,
        smooth: true,
        lineStyle: { color: '#FB8C00', width: 2, type: 'dashed' },
        itemStyle: { color: '#FB8C00' },
        areaStyle: { color: 'rgba(251,140,0,0.08)' },
      },
    ],
  }
  chartInstance.setOption(option)
}

async function handleForecast() {
  const series = parseSeries(seriesInput.value)
  if (!series || series.length < 3) {
    ElMessage.warning('请输入至少3个有效数值的历史数据')
    return
  }
  loading.value = true
  result.value = undefined
  try {
    result.value = await forecast({
      deviceCode: form.deviceCode || undefined,
      pipeType: form.pipeType || undefined,
      metricType: form.metricType || undefined,
      series,
      horizonDays: form.horizonDays,
    })
    if (result.value) {
      await nextTick()
      renderChart(result.value, series)
    }
  } catch {
    result.value = null
  } finally {
    loading.value = false
  }
}

function handleReset() {
  form.deviceCode = ''
  form.pipeType = ''
  form.metricType = ''
  form.horizonDays = 7
  seriesInput.value = ''
  result.value = undefined
  if (chartInstance) { chartInstance.dispose(); chartInstance = null }
}

function handleFillDemo() {
  form.deviceCode = 'DEV-GAS-001'
  form.pipeType = 'gas'
  form.metricType = 'pressure'
  form.horizonDays = 7
  seriesInput.value = '1.2, 1.3, 1.1, 1.4, 1.5, 1.3, 1.6, 1.8, 1.7, 2.0, 2.1, 1.9, 2.3, 2.5, 2.4'
}

onUnmounted(() => {
  if (chartInstance) { chartInstance.dispose(); chartInstance = null }
})
</script>

<style scoped lang="scss">
.page-container {
  padding: $spacing-md;
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
}

.form-card {
  margin-bottom: $spacing-md;
}

.forecast-form {
  padding-top: $spacing-sm;
}

.overview-row {
  margin-bottom: $spacing-md;
}

.overview-card {
  text-align: center;

  &__label {
    font-size: 13px;
    color: $text-secondary;
    margin-bottom: $spacing-xs;
  }

  &__value {
    font-size: 18px;
    font-weight: 700;
    color: $text-primary;
  }
}

.text-ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 200px;
  display: inline-block;
}

.chart-card {
  margin-bottom: $spacing-md;
}

.chart-container {
  width: 100%;
  height: 380px;
}

.report-card {
  margin-bottom: $spacing-md;
}

.report-content {
  font-size: 14px;
  color: $text-regular;
  line-height: 1.8;
  white-space: pre-wrap;
}

.degraded-card {
  border-left: 4px solid $warning-color;
}
</style>
