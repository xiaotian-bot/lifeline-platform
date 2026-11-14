<template>
  <div class="detail-page" v-loading="pageLoading">
    <!-- 返回按钮 + 项目信息卡片 -->
    <div class="page-header">
      <el-button @click="router.back()" plain size="small">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
    </div>

    <div v-if="detail" class="project-info-card">
      <div class="info-title">
        <h2>{{ detail.project.projectName }}</h2>
        <StatusTag :status="String(detail.project.status)" :label="detail.project.statusText || statusLabel(detail.project.status)" />
      </div>
      <el-descriptions :column="4" border size="small">
        <el-descriptions-item label="施工地点">{{ detail.project.location || '-' }}</el-descriptions-item>
        <el-descriptions-item label="施工单位">{{ detail.project.contractor || '-' }}</el-descriptions-item>
        <el-descriptions-item label="监理负责人">{{ detail.project.supervisor || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.project.statusText || statusLabel(detail.project.status) }}</el-descriptions-item>
        <el-descriptions-item label="计划开工">{{ detail.project.startDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="计划完工">{{ detail.project.endDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="经度">{{ detail.project.longitude ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="纬度">{{ detail.project.latitude ?? '-' }}</el-descriptions-item>
      </el-descriptions>
    </div>

    <!-- Tabs -->
    <div v-if="detail" class="tabs-container">
      <el-tabs v-model="activeTab">
        <!-- 工序质检 -->
        <el-tab-pane label="工序质检" name="process">
          <div class="tab-toolbar">
            <el-button type="primary" size="small" @click="openProcessDialog">添加工序</el-button>
          </div>
          <el-table :data="detail.processes" border stripe size="small">
            <el-table-column prop="processName" label="工序名称" min-width="140" />
            <el-table-column prop="processType" label="工序类型" width="120" align="center">
              <template #default="{ row }">{{ processTypeLabel(row.processType) }}</template>
            </el-table-column>
            <el-table-column label="质检状态" width="110" align="center">
              <template #default="{ row }">
                <StatusTag :status="String(row.status)" :label="processStatusLabel(row.status)" />
              </template>
            </el-table-column>
            <el-table-column label="质检得分" width="100" align="center">
              <template #default="{ row }">
                <span v-if="row.qualityScore != null" :style="{ color: scoreColor(row.qualityScore), fontWeight: 600 }">
                  {{ row.qualityScore }}
                </span>
                <span v-else class="text-muted">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="inspector" label="质检人" width="120" />
            <el-table-column prop="inspectTime" label="质检时间" width="170" />
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status <= 1" link type="primary" size="small" @click="openInspectDialog(row)">人工质检</el-button>
                <el-button v-if="row.status <= 1" link type="success" size="small" @click="openVisionDialog(row)">AI视频分析</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- AI视频分析 -->
        <el-tab-pane label="AI视频分析" name="vision">
          <div class="tab-toolbar">
            <el-button type="primary" size="small" @click="triggerVisionAnalysis" :disabled="!detail.processes.length">
              触发AI分析
            </el-button>
          </div>
          <div v-if="visionResults.length === 0" class="empty-tip">
            <el-empty description="暂无分析结果，请先触发AI分析" />
          </div>
          <el-table v-else :data="visionResults" border stripe size="small">
            <el-table-column prop="processId" label="工序ID" width="100" align="center" />
            <el-table-column label="分析结论" min-width="160">
              <template #default="{ row }">
                <span v-if="row.degraded" class="degraded-tag">降级-转人工复核</span>
                <span v-else-if="row.violation" style="color: #E53935">识别到违规: {{ row.violationType }}</span>
                <span v-else style="color: #43A047">合规</span>
              </template>
            </el-table-column>
            <el-table-column label="置信度" width="100" align="center">
              <template #default="{ row }">
                {{ row.confidence != null ? (row.confidence * 100).toFixed(1) + '%' : '-' }}
              </template>
            </el-table-column>
            <el-table-column label="质检得分" width="100" align="center">
              <template #default="{ row }">
                <span v-if="row.qualityScore != null" :style="{ color: scoreColor(row.qualityScore), fontWeight: 600 }">
                  {{ row.qualityScore }}
                </span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
          </el-table>
        </el-tab-pane>

        <!-- 违规记录 -->
        <el-tab-pane label="违规记录" name="violation">
          <div class="tab-toolbar">
            <el-button type="danger" size="small" plain @click="openViolationDialog">上报违规</el-button>
          </div>
          <el-table :data="detail.violations" border stripe size="small">
            <el-table-column prop="violationType" label="违规类型" width="140" />
            <el-table-column prop="description" label="违规描述" min-width="200" show-overflow-tooltip />
            <el-table-column label="处理状态" width="100" align="center">
              <template #default="{ row }">
                <StatusTag :status="row.status === 0 ? 'pending' : 'completed'" :label="row.status === 0 ? '待处理' : '已处理'" />
              </template>
            </el-table-column>
            <el-table-column label="区块链存证" width="120" align="center">
              <template #default="{ row }">
                <span v-if="row.blockchainHash" class="evidence-badge evidence-yes">
                  <el-icon><CircleCheck /></el-icon> 已存证
                </span>
                <span v-else class="evidence-badge evidence-no">
                  <el-icon><Remove /></el-icon> 未存证
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="上报时间" width="170" />
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status === 0" link type="warning" size="small" @click="handleViolationAction(row)">处理</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 影像资料 -->
        <el-tab-pane label="影像资料" name="media">
          <div class="tab-toolbar">
            <el-select v-model="mediaFilter" placeholder="全部类型" clearable size="small" style="width: 120px; margin-right: 12px">
              <el-option label="图片" value="image" />
              <el-option label="视频" value="video" />
            </el-select>
            <el-upload
              :show-file-list="false"
              :before-upload="handleBeforeUpload"
              :http-request="handleUploadRequest"
              accept="image/*,video/*"
            >
              <el-button type="primary" size="small" plain>上传影像</el-button>
            </el-upload>
          </div>
          <div v-if="filteredMedia.length === 0" class="empty-tip">
            <el-empty description="暂无影像资料" />
          </div>
          <div v-else class="media-grid">
            <div v-for="item in filteredMedia" :key="item.id" class="media-card">
              <div class="media-preview">
                <img v-if="item.mediaType === 'image'" :src="item.fileUrl" alt="影像" />
                <video v-else :src="item.fileUrl" muted />
              </div>
              <div class="media-info">
                <span class="media-type-tag">{{ item.mediaType === 'image' ? '图片' : '视频' }}</span>
                <span class="media-time">{{ item.captureTime || item.createdAt }}</span>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 添加工序对话框 -->
    <el-dialog v-model="processDialogVisible" title="添加工序" width="440px" :close-on-click-modal="false">
      <el-form :model="processForm" :rules="processRules" ref="processFormRef" label-width="90px">
        <el-form-item label="工序名称" prop="processName">
          <el-input v-model="processForm.processName" placeholder="请输入工序名称" />
        </el-form-item>
        <el-form-item label="工序类型" prop="processType">
          <el-select v-model="processForm.processType" placeholder="请选择" style="width: 100%">
            <el-option label="开挖" value="EXCAVATION" />
            <el-option label="管道铺设" value="PIPE_LAYING" />
            <el-option label="回填" value="BACKFILL" />
            <el-option label="路面恢复" value="RESTORE" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="processDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleCreateProcess">确认</el-button>
      </template>
    </el-dialog>

    <!-- 人工质检对话框 -->
    <el-dialog v-model="inspectDialogVisible" title="人工质检" width="440px" :close-on-click-modal="false">
      <el-form :model="inspectForm" :rules="inspectRules" ref="inspectFormRef" label-width="90px">
        <el-form-item label="工序">{{ inspectForm.processName }}</el-form-item>
        <el-form-item label="质检人" prop="inspector">
          <el-input v-model="inspectForm.inspector" placeholder="请输入质检人" />
        </el-form-item>
        <el-form-item label="质检得分" prop="qualityScore">
          <el-input-number v-model="inspectForm.qualityScore" :min="0" :max="100" :precision="1" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="inspectDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleInspect">提交</el-button>
      </template>
    </el-dialog>

    <!-- AI视频分析对话框 -->
    <el-dialog v-model="visionDialogVisible" title="AI视频分析" width="480px" :close-on-click-modal="false">
      <el-form :model="visionForm" label-width="90px">
        <el-form-item label="工序">{{ visionForm.processName }}</el-form-item>
        <el-form-item label="影像地址" required>
          <el-input v-model="visionForm.mediaUrl" placeholder="请输入影像对象键/URL" />
        </el-form-item>
        <el-form-item label="影像类型">
          <el-select v-model="visionForm.mediaType" style="width: 100%">
            <el-option label="图片" value="image" />
            <el-option label="视频" value="video" />
          </el-select>
        </el-form-item>
        <el-form-item label="摄像头ID">
          <el-input v-model="visionForm.cameraId" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visionDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleVisionInspect">开始分析</el-button>
      </template>
    </el-dialog>

    <!-- 上报违规对话框 -->
    <el-dialog v-model="violationDialogVisible" title="上报违规" width="480px" :close-on-click-modal="false">
      <el-form :model="violationForm" :rules="violationRules" ref="violationFormRef" label-width="90px">
        <el-form-item label="违规类型" prop="violationType">
          <el-select v-model="violationForm.violationType" placeholder="请选择" style="width: 100%">
            <el-option label="未戴安全帽" value="NO_HELMET" />
            <el-option label="围挡缺失" value="NO_FENCE" />
            <el-option label="擅自开挖" value="UNAUTHORIZED_EXCAVATION" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="违规描述" prop="description">
          <el-input v-model="violationForm.description" type="textarea" :rows="3" placeholder="请描述违规情况" />
        </el-form-item>
        <el-form-item label="证据地址">
          <el-input v-model="violationForm.evidenceUrl" placeholder="证据影像URL（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="violationDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleReportViolation">上报</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { ArrowLeft, CircleCheck, Remove } from '@element-plus/icons-vue'
import StatusTag from '@/components/common/StatusTag.vue'
import {
  getConstructionDetail,
  createProcess,
  inspectProcess,
  visionInspect,
  reportViolation,
  handleViolation as handleViolationApi,
  uploadMedia,
} from '@/api/construction'
import type {
  ProjectDetail,
  ConstructionProcess,
  ConstructionViolation,
  ConstructionMedia,
  VisionInspectResult,
} from '@/types/construction.d'

const route = useRoute()
const router = useRouter()
const projectId = Number(route.params.id)

/* ==================== 状态映射 ==================== */
const statusTextMap: Record<number, string> = { 0: '待开工', 1: '施工中', 2: '已完工', 3: '已停工' }
function statusLabel(s: number): string { return statusTextMap[s] || '未知' }

const processTypeMap: Record<string, string> = {
  EXCAVATION: '开挖', PIPE_LAYING: '管道铺设', BACKFILL: '回填', RESTORE: '路面恢复',
}
function processTypeLabel(t: string): string { return processTypeMap[t] || t }

const processStatusMap: Record<number, string> = { 0: '待质检', 1: '质检中', 2: '质检通过', 3: '质检不通过' }
function processStatusLabel(s: number): string { return processStatusMap[s] || '未知' }

function scoreColor(score: number): string {
  if (score >= 90) return '#43A047'
  if (score >= 70) return '#FB8C00'
  return '#E53935'
}

/* ==================== 页面数据 ==================== */
const pageLoading = ref(false)
const detail = ref<ProjectDetail | null>(null)
const activeTab = ref('process')
const visionResults = ref<VisionInspectResult[]>([])

async function loadDetail() {
  pageLoading.value = true
  try {
    detail.value = await getConstructionDetail(projectId)
  } catch {
    ElMessage.error('加载项目详情失败')
  } finally {
    pageLoading.value = false
  }
}

/* ==================== 工序管理 ==================== */
const processDialogVisible = ref(false)
const processFormRef = ref<FormInstance>()
const processForm = reactive({ processName: '', processType: '' })
const processRules: FormRules = {
  processName: [{ required: true, message: '请输入工序名称', trigger: 'blur' }],
  processType: [{ required: true, message: '请选择工序类型', trigger: 'change' }],
}

function openProcessDialog() {
  processForm.processName = ''
  processForm.processType = ''
  processDialogVisible.value = true
}

const submitLoading = ref(false)

async function handleCreateProcess() {
  if (!processFormRef.value) return
  await processFormRef.value.validate()
  submitLoading.value = true
  try {
    await createProcess(projectId, { processName: processForm.processName, processType: processForm.processType })
    ElMessage.success('工序创建成功')
    processDialogVisible.value = false
    loadDetail()
  } catch { /* handled */ } finally {
    submitLoading.value = false
  }
}

/* ==================== 人工质检 ==================== */
const inspectDialogVisible = ref(false)
const inspectFormRef = ref<FormInstance>()
const inspectForm = reactive({ processId: 0, processName: '', inspector: '', qualityScore: 80 })
const inspectRules: FormRules = {
  inspector: [{ required: true, message: '请输入质检人', trigger: 'blur' }],
  qualityScore: [{ required: true, message: '请输入质检得分', trigger: 'blur' }],
}

function openInspectDialog(row: ConstructionProcess) {
  inspectForm.processId = row.id
  inspectForm.processName = row.processName
  inspectForm.inspector = ''
  inspectForm.qualityScore = 80
  inspectDialogVisible.value = true
}

async function handleInspect() {
  if (!inspectFormRef.value) return
  await inspectFormRef.value.validate()
  submitLoading.value = true
  try {
    await inspectProcess(inspectForm.processId, { inspector: inspectForm.inspector, qualityScore: inspectForm.qualityScore })
    ElMessage.success('质检记录提交成功')
    inspectDialogVisible.value = false
    loadDetail()
  } catch { /* handled */ } finally {
    submitLoading.value = false
  }
}

/* ==================== AI视频分析 ==================== */
const visionDialogVisible = ref(false)
const visionForm = reactive({ processId: 0, processName: '', mediaUrl: '', mediaType: 'image', cameraId: '' })

function openVisionDialog(row: ConstructionProcess) {
  visionForm.processId = row.id
  visionForm.processName = row.processName
  visionForm.mediaUrl = ''
  visionForm.mediaType = 'image'
  visionForm.cameraId = ''
  visionDialogVisible.value = true
}

async function handleVisionInspect() {
  if (!visionForm.mediaUrl) {
    ElMessage.warning('请输入影像地址')
    return
  }
  submitLoading.value = true
  try {
    const result = await visionInspect(visionForm.processId, {
      mediaUrl: visionForm.mediaUrl,
      mediaType: visionForm.mediaType,
      cameraId: visionForm.cameraId || undefined,
    })
    visionResults.value.unshift(result)
    ElMessage.success(result.degraded ? 'AI服务降级，已转人工复核' : '分析完成')
    visionDialogVisible.value = false
    loadDetail()
  } catch { /* handled */ } finally {
    submitLoading.value = false
  }
}

function triggerVisionAnalysis() {
  // 取第一个待质检工序触发
  const pending = detail.value?.processes.find(p => p.status <= 1)
  if (!pending) {
    ElMessage.warning('没有可分析的工序')
    return
  }
  openVisionDialog(pending)
}

/* ==================== 违规记录 ==================== */
const violationDialogVisible = ref(false)
const violationFormRef = ref<FormInstance>()
const violationForm = reactive({ violationType: '', description: '', evidenceUrl: '' })
const violationRules: FormRules = {
  violationType: [{ required: true, message: '请选择违规类型', trigger: 'change' }],
  description: [{ required: true, message: '请输入违规描述', trigger: 'blur' }],
}

function openViolationDialog() {
  violationForm.violationType = ''
  violationForm.description = ''
  violationForm.evidenceUrl = ''
  violationDialogVisible.value = true
}

async function handleReportViolation() {
  if (!violationFormRef.value) return
  await violationFormRef.value.validate()
  submitLoading.value = true
  try {
    await reportViolation(projectId, {
      violationType: violationForm.violationType,
      description: violationForm.description,
      evidenceUrl: violationForm.evidenceUrl || undefined,
    })
    ElMessage.success('违规上报成功')
    violationDialogVisible.value = false
    loadDetail()
  } catch { /* handled */ } finally {
    submitLoading.value = false
  }
}

async function handleViolationAction(row: ConstructionViolation) {
  try {
    await ElMessageBox.confirm('确认处理该违规记录？', '处理确认', { type: 'warning' })
    await handleViolationApi(row.id)
    ElMessage.success('违规已处理')
    loadDetail()
  } catch { /* cancel or error */ }
}

/* ==================== 影像资料 ==================== */
const mediaFilter = ref('')

const filteredMedia = computed<ConstructionMedia[]>(() => {
  if (!detail.value?.media) return []
  if (!mediaFilter.value) return detail.value.media
  return detail.value.media.filter(m => m.mediaType === mediaFilter.value)
})

function handleBeforeUpload(file: File): boolean {
  const isImage = file.type.startsWith('image/')
  const isVideo = file.type.startsWith('video/')
  if (!isImage && !isVideo) {
    ElMessage.error('仅支持图片和视频文件')
    return false
  }
  if (file.size > 200 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过200MB')
    return false
  }
  return true
}

async function handleUploadRequest(options: { file: File }) {
  try {
    await uploadMedia(projectId, options.file)
    ElMessage.success('影像上传成功')
    loadDetail()
  } catch {
    ElMessage.error('上传失败')
  }
}

/* ==================== 初始化 ==================== */
onMounted(() => {
  loadDetail()
})
</script>

<style scoped lang="scss">
.detail-page {
  padding: $spacing-md;
}

.page-header {
  margin-bottom: $spacing-md;
}

.project-info-card {
  background: #fff;
  border-radius: $border-radius-lg;
  padding: $spacing-lg;
  margin-bottom: $spacing-md;
  box-shadow: $shadow-light;

  .info-title {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    margin-bottom: $spacing-md;

    h2 {
      font-size: 18px;
      font-weight: 600;
      color: $text-primary;
      margin: 0;
    }
  }
}

.tabs-container {
  background: #fff;
  border-radius: $border-radius-lg;
  padding: $spacing-md $spacing-lg;
  box-shadow: $shadow-light;
}

.tab-toolbar {
  display: flex;
  align-items: center;
  margin-bottom: $spacing-md;
}

.empty-tip {
  padding: $spacing-xl 0;
}

/* 区块链存证徽章 */
.evidence-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: $border-radius-sm;
  white-space: nowrap;
}

.evidence-yes {
  color: #43A047;
  background: rgba(67, 160, 71, 0.1);
}

.evidence-no {
  color: #909399;
  background: rgba(144, 147, 153, 0.1);
}

.degraded-tag {
  display: inline-block;
  padding: 2px 8px;
  background: rgba(251, 140, 0, 0.1);
  color: #FB8C00;
  border-radius: $border-radius-sm;
  font-size: 12px;
}

.text-muted {
  color: $text-secondary;
}

/* 影像网格 */
.media-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: $spacing-md;
}

.media-card {
  border: 1px solid #ebeef5;
  border-radius: $border-radius-base;
  overflow: hidden;
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: $shadow-base;
  }

  .media-preview {
    width: 100%;
    height: 150px;
    background: #f5f7fa;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;

    img, video {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  .media-info {
    padding: $spacing-xs $spacing-sm;
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 12px;

    .media-type-tag {
      color: $text-secondary;
      background: #f0f2f5;
      padding: 1px 6px;
      border-radius: $border-radius-sm;
    }

    .media-time {
      color: $text-placeholder;
    }
  }
}
</style>
