<template>
  <div class="page-container">
    <el-card shadow="never" class="form-card">
      <template #header>
        <div class="card-header">
          <span class="card-header__title">时序异常检测</span>
          <el-tag type="info" size="small">AI智能分析</el-tag>
        </div>
      </template>
      <el-form :model="form" label-width="100px" class="detect-form">
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="设备编码">
              <el-input v-model="form.deviceCode" placeholder="如 DEV-GAS-001" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="管网类型">
              <el-select v-model="form.pipeType" placeholder="选择管网类型" clearable style="width: 100%">
                <el-option label="燃气" value="gas" />
                <el-option label="供水" value="water" />
                <el-option label="排水" value="drainage" />
                <el-option label="供热" value="heating" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="指标类型">
              <el-select v-model="form.metricType" placeholder="选择指标类型" clearable style="width: 100%">
                <el-option label="压力" value="pressure" />
                <el-option label="温度" value="temperature" />
                <el-option label="流量" value="flow" />
                <el-option label="振动" value="vibration" />
                <el-option label="腐蚀" value="corrosion" />
                <el-option label="泄漏" value="leakage" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="时序数据">
          <el-input
            v-model="seriesInput"
            type="textarea"
            :rows="3"
            placeholder="输入时序数值，支持逗号分隔或JSON数组格式，如: 1.2, 1.5, 1.3, 8.9, 1.4, 1.1"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleDetect">开始检测</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="info" plain @click="handleFillDemo">填入示例数据</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 检测结果 -->
    <template v-if="result !== undefined">
      <template v-if="result === null">
        <el-card shadow="never" class="result-card degraded-card">
          <el-empty description="AI分析暂不可用，请稍后重试" :image-size="80" />
        </el-card>
      </template>
      <template v-else>
        <el-row :gutter="16">
          <!-- 状态卡片 -->
          <el-col :span="8">
            <el-card shadow="never" class="result-card" :class="result.anomaly ? 'anomaly-yes' : 'anomaly-no'">
              <div class="result-status">
                <el-icon :size="56" :color="result.anomaly ? '#E53935' : '#43A047'">
                  <component :is="result.anomaly ? 'WarningFilled' : 'CircleCheckFilled'" />
                </el-icon>
                <h2 :style="{ color: result.anomaly ? '#E53935' : '#43A047' }">
                  {{ result.anomaly ? '检测到异常' : '运行正常' }}
                </h2>
                <p class="result-status__desc">
                  {{ result.anomaly ? `异常类型: ${anomalyTypeLabel(result.anomalyType)}` : '时序数据未检出异常模式' }}
                </p>
              </div>
            </el-card>
          </el-col>
          <!-- 评分卡片 -->
          <el-col :span="8">
            <el-card shadow="never" class="result-card score-card">
              <div class="score-gauge">
                <div class="score-gauge__ring" :style="scoreRingStyle">
                  <span class="score-gauge__value">{{ (result.anomalyScore * 100).toFixed(1) }}</span>
                  <span class="score-gauge__unit">分</span>
                </div>
                <p class="score-gauge__label">异常得分</p>
                <p class="score-gauge__hint">0=正常 100=严重异常</p>
              </div>
            </el-card>
          </el-col>
          <!-- 详情卡片 -->
          <el-col :span="8">
            <el-card shadow="never" class="result-card detail-card">
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="置信度">{{ (result.confidence * 100).toFixed(1) }}%</el-descriptions-item>
                <el-descriptions-item label="检测模型">{{ result.model || '-' }}</el-descriptions-item>
                <el-descriptions-item label="推理耗时">{{ result.costMs ?? '-' }} ms</el-descriptions-item>
                <el-descriptions-item label="异常位置">
                  <template v-if="result.anomalyIndexes?.length">
                    <el-tag v-for="idx in result.anomalyIndexes" :key="idx" size="small" type="danger" style="margin: 2px">
                      第{{ idx + 1 }}个采样点
                    </el-tag>
                  </template>
                  <span v-else>-</span>
                </el-descriptions-item>
              </el-descriptions>
            </el-card>
          </el-col>
        </el-row>
      </template>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { WarningFilled, CircleCheckFilled } from '@element-plus/icons-vue'
import { anomalyDetect } from '@/api/ai'
import type { AnomalyResult } from '@/types/ai.d'

const form = reactive({
  deviceCode: '',
  pipeType: '',
  metricType: '',
})
const seriesInput = ref('')
const loading = ref(false)
const result = ref<AnomalyResult | null | undefined>(undefined)

const anomalyTypeMap: Record<string, string> = {
  LEAK: '泄漏',
  BURST: '爆管',
  CORROSION: '腐蚀',
  BLOCKAGE: '堵塞',
}
function anomalyTypeLabel(type: string | null) {
  if (!type) return '未知'
  return anomalyTypeMap[type] || type
}

function parseSeries(input: string): number[] | null {
  const trimmed = input.trim()
  if (!trimmed) return null
  try {
    // Try JSON array first
    if (trimmed.startsWith('[')) {
      const arr = JSON.parse(trimmed)
      if (Array.isArray(arr)) return arr.map(Number).filter(n => !isNaN(n))
    }
    // Comma/space separated
    return trimmed.split(/[,\s]+/).map(Number).filter(n => !isNaN(n))
  } catch {
    return null
  }
}

const scoreRingStyle = computed(() => {
  if (result.value === null || result.value === undefined) return {}
  const score = result.value.anomalyScore
  const pct = score * 100
  const color = pct > 70 ? '#E53935' : pct > 40 ? '#FB8C00' : '#43A047'
  const deg = pct * 3.6
  return {
    background: `conic-gradient(${color} ${deg}deg, #EBEEF5 ${deg}deg)`,
  }
})

async function handleDetect() {
  const series = parseSeries(seriesInput.value)
  if (!series || series.length < 3) {
    ElMessage.warning('请输入至少3个有效数值的时序数据')
    return
  }
  loading.value = true
  result.value = undefined
  try {
    result.value = await anomalyDetect({
      deviceCode: form.deviceCode || undefined,
      pipeType: form.pipeType || undefined,
      metricType: form.metricType || undefined,
      series,
    })
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
  seriesInput.value = ''
  result.value = undefined
}

function handleFillDemo() {
  form.deviceCode = 'DEV-GAS-001'
  form.pipeType = 'gas'
  form.metricType = 'pressure'
  seriesInput.value = '1.2, 1.3, 1.1, 1.4, 1.2, 1.3, 8.7, 1.1, 1.2, 1.3, 1.0, 1.2, 1.1, 1.3, 1.2, 1.4, 1.1, 1.2, 1.3, 1.1'
}
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

.detect-form {
  padding-top: $spacing-sm;
}

.result-card {
  margin-bottom: $spacing-md;
  min-height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;

  :deep(.el-card__body) {
    width: 100%;
  }
}

.anomaly-yes {
  border-left: 4px solid $danger-color;
}

.anomaly-no {
  border-left: 4px solid $success-color;
}

.degraded-card {
  border-left: 4px solid $warning-color;
}

.result-status {
  text-align: center;

  h2 {
    margin-top: $spacing-sm;
    font-size: 20px;
  }

  &__desc {
    color: $text-secondary;
    font-size: 13px;
    margin-top: $spacing-xs;
  }
}

.score-gauge {
  text-align: center;

  &__ring {
    width: 120px;
    height: 120px;
    border-radius: 50%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    margin: 0 auto;
    position: relative;

    &::after {
      content: '';
      position: absolute;
      inset: 10px;
      border-radius: 50%;
      background: #fff;
    }
  }

  &__value {
    position: relative;
    z-index: 1;
    font-size: 28px;
    font-weight: 700;
    color: $text-primary;
    line-height: 1;
  }

  &__unit {
    position: relative;
    z-index: 1;
    font-size: 12px;
    color: $text-secondary;
  }

  &__label {
    font-size: 14px;
    font-weight: 600;
    color: $text-primary;
    margin-top: $spacing-sm;
  }

  &__hint {
    font-size: 12px;
    color: $text-secondary;
    margin-top: 2px;
  }
}
</style>
