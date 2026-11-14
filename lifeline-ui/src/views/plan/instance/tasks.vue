<template>
  <div class="page-container">
    <!-- 返回按钮 & 标题 -->
    <div class="page-header">
      <el-button @click="router.back()" plain size="small">
        <el-icon style="margin-right: 4px"><ArrowLeft /></el-icon>
        返回
      </el-button>
      <h3 class="page-title">任务进度跟踪</h3>
      <el-tag v-if="instanceInfo" type="info" size="small">实例: {{ instanceInfo.planName }}</el-tag>
    </div>

    <!-- 总体进度 -->
    <div class="progress-card">
      <div class="progress-info">
        <span class="progress-label">总体完成进度</span>
        <span class="progress-text">{{ completedCount }} / {{ tasks.length }} 任务</span>
      </div>
      <el-progress
        :percentage="progressPercent"
        :status="progressPercent === 100 ? 'success' : undefined"
        :stroke-width="16"
        striped
        striped-flow
      />
      <div class="progress-legend">
        <span class="legend-item"><i class="dot dot-pending"></i>待执行 {{ pendingCount }}</span>
        <span class="legend-item"><i class="dot dot-running"></i>执行中 {{ runningCount }}</span>
        <span class="legend-item"><i class="dot dot-done"></i>已完成 {{ completedCount }}</span>
        <span class="legend-item"><i class="dot dot-cancel"></i>已取消 {{ cancelledCount }}</span>
      </div>
    </div>

    <!-- 任务列表 -->
    <div v-loading="loading" class="task-list">
      <div
        v-for="task in tasks"
        :key="task.id"
        class="task-item"
        :class="`task-item--${taskStatusClass(task.status)}`"
      >
        <div class="task-item__header">
          <div class="task-item__name">
            <el-icon class="task-status-icon" :style="{ color: taskDotColor(task.status) }">
              <CircleCheck v-if="task.status === 2" />
              <Loading v-else-if="task.status === 1" />
              <Clock v-else-if="task.status === 0" />
              <CircleClose v-else />
            </el-icon>
            {{ task.taskName }}
          </div>
          <el-tag :type="priorityType(task.priority)" size="small">{{ priorityLabel(task.priority) }}</el-tag>
        </div>
        <div class="task-item__body">
          <div class="task-meta">
            <span><b>类型:</b> {{ task.taskType || '-' }}</span>
            <span><b>负责人:</b> {{ task.assignee || '-' }}</span>
            <span><b>开始:</b> {{ task.startTime || '-' }}</span>
            <span><b>结束:</b> {{ task.endTime || '-' }}</span>
          </div>
          <div class="task-item__actions">
            <el-tag :type="taskStatusType(task.status)" size="small">{{ task.statusText }}</el-tag>
            <div class="task-btns">
              <el-button
                v-if="task.status === 0"
                type="primary"
                size="small"
                plain
                @click="handleUpdateTask(task, 1)"
              >开始执行</el-button>
              <el-button
                v-if="task.status === 1"
                type="success"
                size="small"
                plain
                @click="handleUpdateTask(task, 2)"
              >完成任务</el-button>
              <el-button
                v-if="task.status === 0 || task.status === 1"
                type="danger"
                size="small"
                plain
                @click="handleUpdateTask(task, 3)"
              >取消</el-button>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-if="!loading && tasks.length === 0" description="暂无任务数据" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, CircleCheck, Loading, Clock, CircleClose } from '@element-plus/icons-vue'
import { fetchPlanTasks, fetchPlanDetail, updatePlanTask } from '@/api/plan'
import type { PlanTaskVO, PlanInstanceVO } from '@/types/plan.d'

const route = useRoute()
const router = useRouter()

const instanceId = computed(() => Number(route.params.id))

const loading = ref(false)
const tasks = ref<PlanTaskVO[]>([])
const instanceInfo = ref<PlanInstanceVO | null>(null)

/* ==================== 映射 ==================== */
function priorityLabel(p: number): string {
  const map: Record<number, string> = { 1: '高', 2: '中', 3: '低' }
  return map[p] ?? String(p)
}

function priorityType(p: number): string {
  const map: Record<number, string> = { 1: 'danger', 2: 'warning', 3: 'info' }
  return map[p] ?? 'info'
}

function taskStatusType(status: number): string {
  const map: Record<number, string> = { 0: 'info', 1: '', 2: 'success', 3: 'danger' }
  return map[status] ?? 'info'
}

function taskStatusClass(status: number): string {
  const map: Record<number, string> = { 0: 'pending', 1: 'running', 2: 'done', 3: 'cancelled' }
  return map[status] ?? 'pending'
}

function taskDotColor(status: number): string {
  const map: Record<number, string> = { 0: '#909399', 1: '#1E88E5', 2: '#43A047', 3: '#E53935' }
  return map[status] ?? '#909399'
}

/* ==================== 统计 ==================== */
const pendingCount = computed(() => tasks.value.filter(t => t.status === 0).length)
const runningCount = computed(() => tasks.value.filter(t => t.status === 1).length)
const completedCount = computed(() => tasks.value.filter(t => t.status === 2).length)
const cancelledCount = computed(() => tasks.value.filter(t => t.status === 3).length)
const progressPercent = computed(() => {
  if (!tasks.value.length) return 0
  return Math.round((completedCount.value / tasks.value.length) * 100)
})

/* ==================== 加载 ==================== */
async function loadData() {
  loading.value = true
  try {
    const [taskList, detail] = await Promise.all([
      fetchPlanTasks(instanceId.value),
      fetchPlanDetail(instanceId.value),
    ])
    tasks.value = taskList
    instanceInfo.value = detail.instance
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

/* ==================== 任务操作 ==================== */
async function handleUpdateTask(task: PlanTaskVO, targetStatus: number) {
  const actionMap: Record<number, string> = { 1: '开始执行', 2: '完成', 3: '取消' }
  try {
    await ElMessageBox.confirm(`确认将任务「${task.taskName}」${actionMap[targetStatus]}？`, '操作确认', { type: 'info' })
    await updatePlanTask(task.id, { status: targetStatus })
    ElMessage.success('任务状态已更新')
    loadData()
  } catch {
    // cancel or error
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.page-container {
  padding: $spacing-md;
}

.page-header {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-bottom: $spacing-md;
}

.page-title {
  font-size: 16px;
  font-weight: 600;
  color: $text-primary;
  margin: 0;
  flex: 1;
}

.progress-card {
  background: #fff;
  border-radius: $border-radius-lg;
  padding: $spacing-lg;
  box-shadow: $shadow-light;
  margin-bottom: $spacing-md;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-sm;
}

.progress-label {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
}

.progress-text {
  font-size: 13px;
  color: $text-secondary;
}

.progress-legend {
  display: flex;
  gap: $spacing-lg;
  margin-top: $spacing-md;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: $text-secondary;

  .dot {
    display: inline-block;
    width: 8px;
    height: 8px;
    border-radius: 50%;

    &-pending { background: #909399; }
    &-running { background: #1E88E5; }
    &-done { background: #43A047; }
    &-cancel { background: #E53935; }
  }
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.task-item {
  background: #fff;
  border-radius: $border-radius-lg;
  border: 1px solid #e8eaec;
  overflow: hidden;
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: $shadow-base;
  }

  &--done {
    border-left: 3px solid #43A047;
  }

  &--running {
    border-left: 3px solid #1E88E5;
  }

  &--pending {
    border-left: 3px solid #909399;
  }

  &--cancelled {
    border-left: 3px solid #E53935;
    opacity: 0.7;
  }

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: $spacing-sm $spacing-md;
    background: #fafafa;
    border-bottom: 1px solid #f0f0f0;
  }

  &__name {
    display: flex;
    align-items: center;
    gap: $spacing-xs;
    font-size: 14px;
    font-weight: 600;
    color: $text-primary;
  }

  &__body {
    padding: $spacing-md;
  }

  &__actions {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: $spacing-sm;
  }
}

.task-status-icon {
  font-size: 16px;
}

.task-meta {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-md;
  font-size: 12px;
  color: $text-secondary;

  b {
    color: $text-regular;
    margin-right: 2px;
  }
}

.task-btns {
  display: flex;
  gap: $spacing-xs;
}
</style>
