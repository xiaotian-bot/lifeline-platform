<template>
  <div class="page-container">
    <el-row :gutter="16">
      <!-- 左侧：上传与分析 -->
      <el-col :span="14">
        <el-card shadow="never" class="upload-card">
          <template #header>
            <div class="card-header">
              <span class="card-header__title">施工影像分析</span>
              <el-tag type="info" size="small">AI视觉识别</el-tag>
            </div>
          </template>

          <!-- 上传区域 -->
          <el-upload
            ref="uploadRef"
            class="vision-upload"
            drag
            :auto-upload="false"
            :show-file-list="false"
            accept="image/*"
            :on-change="handleFileChange"
          >
            <div v-if="!previewUrl" class="upload-placeholder">
              <el-icon :size="48" color="#C0C4CC"><UploadFilled /></el-icon>
              <p class="upload-text">将影像文件拖到此处，或<em>点击上传</em></p>
              <p class="upload-hint">支持 JPG/PNG 格式图片</p>
            </div>
            <img v-else :src="previewUrl" class="preview-image" />
          </el-upload>

          <!-- 表单 -->
          <el-form :model="form" inline class="analyze-form">
            <el-form-item label="摄像头ID">
              <el-input v-model="form.cameraId" placeholder="如 CAM-001" clearable style="width: 140px" />
            </el-form-item>
            <el-form-item label="施工项目ID">
              <el-input-number v-model="form.projectId" :min="0" placeholder="项目ID" style="width: 130px" controls-position="right" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" :disabled="!previewUrl" @click="handleAnalyze">
                开始分析
              </el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 分析结果 -->
        <template v-if="result !== undefined">
          <template v-if="result === null">
            <el-card shadow="never" class="result-card degraded-card">
              <el-empty description="AI分析暂不可用，请稍后重试" :image-size="80" />
            </el-card>
          </template>
          <template v-else>
            <el-card shadow="never" class="result-card" :class="result.violation ? 'violation-yes' : 'violation-no'">
              <template #header>
                <div class="card-header">
                  <span class="card-header__title">分析结果</span>
                </div>
              </template>
              <div class="result-summary">
                <div class="result-icon">
                  <el-icon v-if="result.violation" :size="40" color="#E53935"><WarningFilled /></el-icon>
                  <el-icon v-else :size="40" color="#43A047"><CircleCheckFilled /></el-icon>
                </div>
                <div class="result-info">
                  <h3 :style="{ color: result.violation ? '#E53935' : '#43A047' }">
                    {{ result.violation ? '检测到违规行为' : '未检测到违规' }}
                  </h3>
                  <p v-if="result.description" class="result-desc">{{ result.description }}</p>
                  <div class="result-meta">
                    <el-tag v-if="result.violationType" type="danger" size="small">
                      {{ violationTypeLabel(result.violationType) }}
                    </el-tag>
                    <span class="confidence">置信度: {{ (result.confidence * 100).toFixed(1) }}%</span>
                  </div>
                </div>
              </div>
              <div v-if="result.evidenceUrl" class="evidence-link">
                <el-link type="primary" :href="result.evidenceUrl" target="_blank">查看证据影像</el-link>
              </div>
            </el-card>
          </template>
        </template>
      </el-col>

      <!-- 右侧：历史记录 -->
      <el-col :span="10">
        <el-card shadow="never" class="history-card">
          <template #header>
            <div class="card-header">
              <span class="card-header__title">分析历史</span>
              <el-tag size="small" type="info">最近{{ historyList.length }}条</el-tag>
            </div>
          </template>
          <div v-if="historyList.length" class="history-list">
            <div v-for="(item, idx) in historyList" :key="idx" class="history-item">
              <div class="history-item__preview">
                <img v-if="item.previewUrl" :src="item.previewUrl" class="history-thumb" />
                <div v-else class="history-thumb-placeholder">
                  <el-icon><Picture /></el-icon>
                </div>
              </div>
              <div class="history-item__info">
                <div class="history-item__status">
                  <el-tag :type="item.violation ? 'danger' : 'success'" size="small">
                    {{ item.violation ? '违规' : '正常' }}
                  </el-tag>
                  <span v-if="item.violationType" class="violation-type">{{ violationTypeLabel(item.violationType) }}</span>
                </div>
                <div class="history-item__time">{{ item.time }}</div>
                <div class="history-item__confidence">置信度: {{ (item.confidence * 100).toFixed(1) }}%</div>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无分析记录" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import type { UploadFile } from 'element-plus'
import { UploadFilled, WarningFilled, CircleCheckFilled, Picture } from '@element-plus/icons-vue'
import { visionAnalyze } from '@/api/ai'
import type { VisionAnalyzeResult } from '@/types/ai.d'

interface HistoryItem {
  previewUrl: string
  violation: boolean
  violationType: string | null
  confidence: number
  time: string
}

const form = reactive({
  cameraId: '',
  projectId: 0,
})
const loading = ref(false)
const previewUrl = ref('')
const result = ref<VisionAnalyzeResult | null | undefined>(undefined)
const historyList = ref<HistoryItem[]>([])
let rawFile: File | null = null

const violationTypeMap: Record<string, string> = {
  NO_HELMET: '未戴安全帽',
  NO_FENCE: '围挡缺失',
  UNAUTHORIZED_EXCAVATION: '擅自开挖',
  NO_SAFETY_VEST: '未穿反光衣',
  ILLEGAL_DUMPING: '违规倾倒',
}
function violationTypeLabel(type: string | null) {
  if (!type) return ''
  return violationTypeMap[type] || type
}

function handleFileChange(file: UploadFile) {
  if (!file.raw) return
  rawFile = file.raw
  const reader = new FileReader()
  reader.onload = (e) => {
    previewUrl.value = e.target?.result as string
  }
  reader.readAsDataURL(file.raw)
  result.value = undefined
}

async function handleAnalyze() {
  if (!previewUrl.value) {
    ElMessage.warning('请先上传影像文件')
    return
  }
  loading.value = true
  result.value = undefined
  try {
    const res = await visionAnalyze({
      cameraId: form.cameraId || undefined,
      projectId: form.projectId || undefined,
      mediaUrl: previewUrl.value,
      mediaType: 'image',
    })
    result.value = res
    // 添加到历史
    if (res) {
      historyList.value.unshift({
        previewUrl: previewUrl.value,
        violation: res.violation,
        violationType: res.violationType,
        confidence: res.confidence,
        time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }),
      })
      if (historyList.value.length > 20) historyList.value.pop()
    }
  } catch {
    result.value = null
  } finally {
    loading.value = false
  }
}

function handleReset() {
  previewUrl.value = ''
  rawFile = null
  result.value = undefined
  form.cameraId = ''
  form.projectId = 0
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

.upload-card {
  margin-bottom: $spacing-md;
}

.vision-upload {
  width: 100%;

  :deep(.el-upload) {
    width: 100%;
  }

  :deep(.el-upload-dragger) {
    width: 100%;
    min-height: 280px;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0;
    overflow: hidden;
  }
}

.upload-placeholder {
  padding: $spacing-xl;
  text-align: center;
}

.upload-text {
  font-size: 14px;
  color: $text-regular;
  margin-top: $spacing-md;

  em { color: $primary-color; font-style: normal; }
}

.upload-hint {
  font-size: 12px;
  color: $text-secondary;
  margin-top: $spacing-xs;
}

.preview-image {
  width: 100%;
  max-height: 400px;
  object-fit: contain;
}

.analyze-form {
  margin-top: $spacing-md;
  padding-top: $spacing-md;
  border-top: 1px solid $border-light;
}

.result-card {
  margin-bottom: $spacing-md;

  :deep(.el-card__body) {
    padding: $spacing-lg;
  }
}

.violation-yes { border-left: 4px solid $danger-color; }
.violation-no { border-left: 4px solid $success-color; }
.degraded-card { border-left: 4px solid $warning-color; }

.result-summary {
  display: flex;
  align-items: flex-start;
  gap: $spacing-lg;
}

.result-icon {
  flex-shrink: 0;
}

.result-info {
  flex: 1;

  h3 { font-size: 18px; margin-bottom: $spacing-sm; }

  .result-desc {
    font-size: 14px;
    color: $text-regular;
    line-height: 1.6;
    margin-bottom: $spacing-sm;
  }

  .result-meta {
    display: flex;
    align-items: center;
    gap: $spacing-md;

    .confidence {
      font-size: 13px;
      color: $text-secondary;
    }
  }
}

.evidence-link {
  margin-top: $spacing-md;
  padding-top: $spacing-sm;
  border-top: 1px dashed $border-light;
}

.history-card {
  position: sticky;
  top: $spacing-md;
}

.history-list {
  max-height: 600px;
  overflow-y: auto;
}

.history-item {
  display: flex;
  gap: $spacing-md;
  padding: $spacing-sm 0;
  border-bottom: 1px solid $border-lighter;

  &:last-child { border-bottom: none; }

  &__preview {
    flex-shrink: 0;
  }

  &__info {
    flex: 1;
    min-width: 0;
  }

  &__status {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    margin-bottom: 4px;

    .violation-type {
      font-size: 12px;
      color: $danger-color;
    }
  }

  &__time {
    font-size: 12px;
    color: $text-secondary;
  }

  &__confidence {
    font-size: 12px;
    color: $text-secondary;
  }
}

.history-thumb {
  width: 64px;
  height: 48px;
  object-fit: cover;
  border-radius: $border-radius-base;
}

.history-thumb-placeholder {
  width: 64px;
  height: 48px;
  background: $bg-color;
  border-radius: $border-radius-base;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $text-placeholder;
}
</style>
