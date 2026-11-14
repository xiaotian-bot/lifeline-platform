<template>
  <div ref="chartRef" class="sensor-chart" :style="{ height: height + 'px' }"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'

interface DataPoint {
  [key: string]: unknown
}

const props = withDefaults(defineProps<{
  data: DataPoint[]
  title?: string
  xAxisKey?: string
  yAxisKey?: string
  height?: number
  color?: string
}>(), {
  xAxisKey: 'time',
  yAxisKey: 'value',
  height: 300,
  color: '#1E88E5',
})

const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null

function initChart() {
  if (!chartRef.value) return
  chart = echarts.init(chartRef.value)
  updateChart()
}

function updateChart() {
  if (!chart) return

  const xData = props.data.map((item) => item[props.xAxisKey] as string)
  const yData = props.data.map((item) => item[props.yAxisKey] as number)

  const option: echarts.EChartsOption = {
    title: props.title ? { text: props.title, left: 'center', textStyle: { fontSize: 14 } } : undefined,
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: xData,
      axisLine: { lineStyle: { color: '#DCDFE6' } },
      axisLabel: { color: '#606266' },
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#EBEEF5' } },
      axisLabel: { color: '#606266' },
    },
    series: [
      {
        type: 'line',
        data: yData,
        smooth: true,
        lineStyle: { color: props.color, width: 2 },
        itemStyle: { color: props.color },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: props.color + '40' },
            { offset: 1, color: props.color + '05' },
          ]),
        },
      },
    ],
  }

  chart.setOption(option)
}

function handleResize() {
  chart?.resize()
}

watch(() => props.data, updateChart, { deep: true })

onMounted(() => {
  initChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
  chart = null
})
</script>

<style scoped>
.sensor-chart {
  width: 100%;
}
</style>
