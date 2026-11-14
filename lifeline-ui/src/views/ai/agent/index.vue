<template>
  <div class="agent-page">
    <!-- 左侧会话列表 -->
    <div class="agent-sidebar">
      <div class="sidebar-header">
        <h3>AI智能助手</h3>
        <el-button type="primary" size="small" plain @click="handleNewChat">
          <el-icon><Plus /></el-icon>新对话
        </el-button>
      </div>
      <div class="conversation-list">
        <div
          v-for="(conv, idx) in conversations"
          :key="idx"
          class="conv-item"
          :class="{ active: currentConvIndex === idx }"
          @click="switchConversation(idx)"
        >
          <el-icon><ChatDotRound /></el-icon>
          <span class="conv-title">{{ conv.title }}</span>
          <span class="conv-time">{{ conv.time }}</span>
        </div>
        <div v-if="!conversations.length" class="conv-empty">暂无对话记录</div>
      </div>
    </div>

    <!-- 右侧主区域 -->
    <div class="agent-main">
      <!-- 标签页 -->
      <div class="agent-tabs">
        <el-radio-group v-model="activeTab" size="default">
          <el-radio-button value="judge">风险研判</el-radio-button>
          <el-radio-button value="plan">预案生成</el-radio-button>
          <el-radio-button value="chat">运维问答</el-radio-button>
        </el-radio-group>
      </div>

      <!-- 消息区域 -->
      <div class="message-area" ref="messageAreaRef">
        <div v-if="!currentMessages.length" class="message-empty">
          <el-icon :size="48" color="#C0C4CC"><ChatDotRound /></el-icon>
          <p>{{ emptyHint }}</p>
        </div>
        <div
          v-for="(msg, idx) in currentMessages"
          :key="idx"
          class="message-item"
          :class="msg.role"
        >
          <div class="message-avatar">
            <el-avatar v-if="msg.role === 'user'" :size="36" style="background: #1E88E5">
              <el-icon><User /></el-icon>
            </el-avatar>
            <el-avatar v-else :size="36" style="background: #43A047">AI</el-avatar>
          </div>
          <div class="message-body">
            <div class="message-role">{{ msg.role === 'user' ? '我' : 'AI助手' }}</div>
            <div class="message-content">
              <template v-if="msg.role === 'user'">
                {{ msg.content }}
              </template>
              <template v-else>
                <!-- 研判结果 -->
                <template v-if="msg.judgeResult">
                  <div class="judge-result">
                    <div v-if="msg.judgeResult.rootCause" class="judge-section">
                      <h4>根因分析</h4>
                      <p>{{ msg.judgeResult.rootCause }}</p>
                    </div>
                    <div v-if="msg.judgeResult.impactScope" class="judge-section">
                      <h4>影响范围</h4>
                      <p>{{ msg.judgeResult.impactScope }}</p>
                    </div>
                    <div v-if="msg.judgeResult.levelReview" class="judge-section">
                      <h4>等级复核</h4>
                      <p>{{ msg.judgeResult.levelReview }}</p>
                    </div>
                    <div v-if="msg.judgeResult.trendPrediction" class="judge-section">
                      <h4>趋势预测</h4>
                      <p>{{ msg.judgeResult.trendPrediction }}</p>
                    </div>
                    <div v-if="msg.judgeResult.handlePriority" class="judge-section">
                      <h4>处置优先级</h4>
                      <el-tag :type="priorityTagType(msg.judgeResult.handlePriority)" size="small">
                        {{ msg.judgeResult.handlePriority }}
                      </el-tag>
                    </div>
                    <div v-if="msg.judgeResult.confidence != null" class="judge-section">
                      <h4>置信度</h4>
                      <el-progress :percentage="Math.round(msg.judgeResult.confidence * 100)" :stroke-width="8" style="max-width: 200px" />
                    </div>
                  </div>
                </template>
                <!-- 预案结果 -->
                <template v-if="msg.planResult">
                  <div class="plan-result">
                    <div v-if="msg.planResult.planName" class="plan-section">
                      <h4>{{ msg.planResult.planName }}</h4>
                    </div>
                    <div v-if="msg.planResult.suggestion" class="plan-section">
                      <h4>处置建议</h4>
                      <p>{{ msg.planResult.suggestion }}</p>
                    </div>
                    <div v-if="msg.planResult.tasks?.length" class="plan-section">
                      <h4>任务清单</h4>
                      <el-timeline>
                        <el-timeline-item v-for="task in msg.planResult.tasks" :key="task.taskOrder" :timestamp="`步骤${task.taskOrder}`">
                          <strong>{{ task.taskName }}</strong>
                          <p v-if="task.description" class="task-desc">{{ task.description }}</p>
                        </el-timeline-item>
                      </el-timeline>
                    </div>
                    <div v-if="msg.planResult.resources?.length" class="plan-section">
                      <h4>资源需求</h4>
                      <el-table :data="msg.planResult.resources" border size="small" style="width: 100%">
                        <el-table-column prop="resourceName" label="资源名称" />
                        <el-table-column prop="quantity" label="数量" width="100" />
                        <el-table-column prop="unit" label="单位" width="80" />
                      </el-table>
                    </div>
                  </div>
                </template>
                <!-- 普通问答 -->
                <template v-if="msg.textContent">
                  <div class="text-content" v-html="formatText(msg.textContent)" />
                  <div v-if="msg.references?.length" class="references">
                    <h5>参考来源</h5>
                    <ul>
                      <li v-for="(ref, ri) in msg.references" :key="ri">{{ ref }}</li>
                    </ul>
                  </div>
                </template>
                <!-- 降级提示 -->
                <template v-if="msg.degraded">
                  <el-alert type="warning" :closable="false" show-icon>
                    <template #title>AI分析暂不可用</template>
                    <p>AI服务当前不可用，请稍后重试。系统已返回降级提示。</p>
                  </el-alert>
                </template>
              </template>
            </div>
            <div class="message-time">{{ msg.time }}</div>
          </div>
        </div>
        <!-- 加载状态 -->
        <div v-if="aiLoading" class="message-item assistant">
          <div class="message-avatar">
            <el-avatar :size="36" style="background: #43A047">AI</el-avatar>
          </div>
          <div class="message-body">
            <div class="message-role">AI助手</div>
            <div class="message-content loading-dots">
              <span></span><span></span><span></span>
              <span class="loading-text">AI分析中，请稍候...</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="input-area">
        <!-- 研判表单 -->
        <template v-if="activeTab === 'judge'">
          <div class="judge-form-inline">
            <el-form inline size="small">
              <el-form-item label="预警ID">
                <el-input-number v-model="judgeForm.warningId" :min="0" placeholder="预警ID" style="width: 120px" controls-position="right" />
              </el-form-item>
              <el-form-item label="管网类型">
                <el-select v-model="judgeForm.pipeType" placeholder="选择" clearable style="width: 100px">
                  <el-option label="燃气" value="gas" />
                  <el-option label="供水" value="water" />
                  <el-option label="排水" value="drainage" />
                  <el-option label="供热" value="heating" />
                </el-select>
              </el-form-item>
              <el-form-item label="标题">
                <el-input v-model="judgeForm.title" placeholder="预警标题" style="width: 160px" />
              </el-form-item>
              <el-form-item label="描述">
                <el-input v-model="judgeForm.description" placeholder="预警描述" style="width: 200px" />
              </el-form-item>
            </el-form>
            <el-button type="primary" :loading="aiLoading" @click="handleJudge">提交研判</el-button>
          </div>
        </template>
        <!-- 预案表单 -->
        <template v-else-if="activeTab === 'plan'">
          <div class="judge-form-inline">
            <el-form inline size="small">
              <el-form-item label="预警ID">
                <el-input-number v-model="planForm.warningId" :min="0" placeholder="预警ID" style="width: 120px" controls-position="right" />
              </el-form-item>
              <el-form-item label="管网类型">
                <el-select v-model="planForm.pipeType" placeholder="选择" clearable style="width: 100px">
                  <el-option label="燃气" value="gas" />
                  <el-option label="供水" value="water" />
                  <el-option label="排水" value="drainage" />
                  <el-option label="供热" value="heating" />
                </el-select>
              </el-form-item>
              <el-form-item label="标题">
                <el-input v-model="planForm.title" placeholder="预警标题" style="width: 160px" />
              </el-form-item>
              <el-form-item label="描述">
                <el-input v-model="planForm.description" placeholder="预警描述" style="width: 200px" />
              </el-form-item>
            </el-form>
            <el-button type="primary" :loading="aiLoading" @click="handlePlan">生成预案</el-button>
          </div>
        </template>
        <!-- 普通问答 -->
        <template v-else>
          <div class="chat-input">
            <el-input
              v-model="chatInput"
              placeholder="输入您的问题，如：燃气管道泄漏应如何处置？"
              :disabled="aiLoading"
              @keyup.enter="handleChat"
            />
            <el-button type="primary" :loading="aiLoading" @click="handleChat">发送</el-button>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, ChatDotRound, User } from '@element-plus/icons-vue'
import { agentJudge, agentPlan, agentChat } from '@/api/ai'
import type { AgentJudgeResult, PlanAgentResult } from '@/types/ai.d'

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  textContent?: string
  judgeResult?: AgentJudgeResult | null
  planResult?: PlanAgentResult | null
  references?: string[]
  degraded?: boolean
  time: string
}

interface Conversation {
  title: string
  time: string
  messages: ChatMessage[]
}

const activeTab = ref<'judge' | 'plan' | 'chat'>('chat')
const aiLoading = ref(false)
const chatInput = ref('')
const messageAreaRef = ref<HTMLDivElement | null>(null)
const currentConvIndex = ref(0)

const conversations = ref<Conversation[]>([])

const judgeForm = reactive({
  warningId: 0,
  pipeType: '',
  title: '',
  description: '',
})

const planForm = reactive({
  warningId: 0,
  pipeType: '',
  title: '',
  description: '',
})

const emptyHint = computed(() => {
  const map = { judge: '提交预警信息进行AI风险研判', plan: '提交预警信息生成智能预案', chat: '输入问题开始AI运维问答' }
  return map[activeTab.value]
})

const currentMessages = computed(() => {
  return conversations.value[currentConvIndex.value]?.messages || []
})

function nowTime() {
  return new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function formatText(text: string): string {
  return text.replace(/\n/g, '<br>')
}

function priorityTagType(p: string): string {
  const map: Record<string, string> = { HIGH: 'danger', MEDIUM: 'warning', LOW: 'info', CRITICAL: 'danger' }
  return map[p] || 'info'
}

function scrollToBottom() {
  nextTick(() => {
    if (messageAreaRef.value) {
      messageAreaRef.value.scrollTop = messageAreaRef.value.scrollHeight
    }
  })
}

function handleNewChat() {
  conversations.value.unshift({
    title: `新对话 ${conversations.value.length + 1}`,
    time: nowTime(),
    messages: [],
  })
  currentConvIndex.value = 0
}

function switchConversation(idx: number) {
  currentConvIndex.value = idx
}

function ensureConversation() {
  if (!conversations.value.length) {
    handleNewChat()
  }
}

function addMessage(msg: ChatMessage) {
  ensureConversation()
  conversations.value[currentConvIndex.value].messages.push(msg)
  if (conversations.value[currentConvIndex.value].messages.length === 1) {
    conversations.value[currentConvIndex.value].title = msg.content.substring(0, 20) + (msg.content.length > 20 ? '...' : '')
  }
  conversations.value[currentConvIndex.value].time = msg.time
  scrollToBottom()
}

// 风险研判
async function handleJudge() {
  if (!judgeForm.warningId) {
    ElMessage.warning('请输入预警ID')
    return
  }
  aiLoading.value = true
  const userMsg: ChatMessage = {
    role: 'user',
    content: `风险研判 - 预警ID: ${judgeForm.warningId}, 管网: ${judgeForm.pipeType || '-'}, 标题: ${judgeForm.title || '-'}`,
    time: nowTime(),
  }
  addMessage(userMsg)

  try {
    const res = await agentJudge({
      warningId: judgeForm.warningId,
      pipeType: judgeForm.pipeType || undefined,
      title: judgeForm.title || undefined,
      description: judgeForm.description || undefined,
    })
    if (res === null || res === undefined) {
      addMessage({ role: 'assistant', content: '', degraded: true, time: nowTime() })
    } else {
      addMessage({ role: 'assistant', content: '', judgeResult: res, time: nowTime() })
    }
  } catch {
    addMessage({ role: 'assistant', content: '', degraded: true, time: nowTime() })
  } finally {
    aiLoading.value = false
  }
}

// 预案生成
async function handlePlan() {
  if (!planForm.warningId) {
    ElMessage.warning('请输入预警ID')
    return
  }
  aiLoading.value = true
  const userMsg: ChatMessage = {
    role: 'user',
    content: `预案生成 - 预警ID: ${planForm.warningId}, 管网: ${planForm.pipeType || '-'}, 标题: ${planForm.title || '-'}`,
    time: nowTime(),
  }
  addMessage(userMsg)

  try {
    const res = await agentPlan({
      warningId: planForm.warningId,
      pipeType: planForm.pipeType || undefined,
      title: planForm.title || undefined,
      description: planForm.description || undefined,
    })
    if (res === null || res === undefined) {
      addMessage({ role: 'assistant', content: '', degraded: true, time: nowTime() })
    } else {
      addMessage({ role: 'assistant', content: '', planResult: res, time: nowTime() })
    }
  } catch {
    addMessage({ role: 'assistant', content: '', degraded: true, time: nowTime() })
  } finally {
    aiLoading.value = false
  }
}

// 运维问答
async function handleChat() {
  const question = chatInput.value.trim()
  if (!question) {
    ElMessage.warning('请输入问题')
    return
  }
  aiLoading.value = true
  chatInput.value = ''
  addMessage({ role: 'user', content: question, time: nowTime() })

  try {
    const res = await agentChat({ question })
    if (res === null || res === undefined) {
      addMessage({ role: 'assistant', content: '', degraded: true, time: nowTime() })
    } else {
      addMessage({
        role: 'assistant',
        content: '',
        textContent: res.answer || '暂无回复',
        references: res.references || undefined,
        time: nowTime(),
      })
    }
  } catch {
    addMessage({ role: 'assistant', content: '', degraded: true, time: nowTime() })
  } finally {
    aiLoading.value = false
  }
}
</script>

<style scoped lang="scss">
.agent-page {
  display: flex;
  height: calc(100vh - 120px);
  padding: $spacing-md;
  gap: $spacing-md;
}

.agent-sidebar {
  width: 260px;
  flex-shrink: 0;
  background: #fff;
  border-radius: $border-radius-lg;
  box-shadow: $shadow-light;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-header {
  padding: $spacing-md;
  border-bottom: 1px solid $border-light;
  display: flex;
  align-items: center;
  justify-content: space-between;

  h3 {
    font-size: 15px;
    font-weight: 600;
    color: $text-primary;
    margin: 0;
  }
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
  padding: $spacing-sm;
}

.conv-item {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-sm $spacing-md;
  border-radius: $border-radius-base;
  cursor: pointer;
  transition: background 0.2s;
  margin-bottom: 2px;

  &:hover { background: $bg-color; }
  &.active { background: rgba($primary-color, 0.08); color: $primary-color; }

  .conv-title {
    flex: 1;
    font-size: 13px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .conv-time {
    font-size: 11px;
    color: $text-secondary;
    flex-shrink: 0;
  }
}

.conv-empty {
  text-align: center;
  color: $text-secondary;
  padding: $spacing-xl;
  font-size: 13px;
}

.agent-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: $border-radius-lg;
  box-shadow: $shadow-light;
  overflow: hidden;
}

.agent-tabs {
  padding: $spacing-md;
  border-bottom: 1px solid $border-light;
}

.message-area {
  flex: 1;
  overflow-y: auto;
  padding: $spacing-md $spacing-lg;
}

.message-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: $text-secondary;

  p { margin-top: $spacing-md; font-size: 14px; }
}

.message-item {
  display: flex;
  gap: $spacing-md;
  margin-bottom: $spacing-lg;

  &.user {
    flex-direction: row-reverse;

    .message-body {
      align-items: flex-end;
    }

    .message-content {
      background: $primary-color;
      color: #fff;
      border-radius: 12px 2px 12px 12px;
    }

    .message-time {
      text-align: right;
    }
  }
}

.message-avatar {
  flex-shrink: 0;
}

.message-body {
  display: flex;
  flex-direction: column;
  max-width: 70%;
}

.message-role {
  font-size: 12px;
  color: $text-secondary;
  margin-bottom: 4px;
}

.message-content {
  padding: $spacing-md;
  border-radius: 2px 12px 12px 12px;
  background: $bg-color;
  font-size: 14px;
  line-height: 1.6;
  color: $text-primary;
}

.message-time {
  font-size: 11px;
  color: $text-placeholder;
  margin-top: 4px;
}

.judge-result, .plan-result {
  .judge-section, .plan-section {
    margin-bottom: $spacing-md;

    h4 {
      font-size: 13px;
      font-weight: 600;
      color: $primary-color;
      margin-bottom: $spacing-xs;
      padding-left: $spacing-sm;
      border-left: 3px solid $primary-color;
    }

    p {
      font-size: 14px;
      color: $text-regular;
      line-height: 1.6;
    }
  }
}

.task-desc {
  font-size: 12px;
  color: $text-secondary;
  margin-top: 2px;
}

.references {
  margin-top: $spacing-md;
  padding-top: $spacing-sm;
  border-top: 1px dashed $border-light;

  h5 {
    font-size: 12px;
    color: $text-secondary;
    margin-bottom: 4px;
  }

  ul {
    list-style: none;
    padding: 0;
    margin: 0;

    li {
      font-size: 12px;
      color: $text-secondary;
      padding: 2px 0;
      &::before { content: '• '; color: $primary-color; }
    }
  }
}

.loading-dots {
  display: flex;
  align-items: center;
  gap: 4px;

  span:not(.loading-text) {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: $primary-color;
    animation: dot-bounce 1.4s infinite ease-in-out both;

    &:nth-child(1) { animation-delay: 0s; }
    &:nth-child(2) { animation-delay: 0.2s; }
    &:nth-child(3) { animation-delay: 0.4s; }
  }

  .loading-text {
    margin-left: $spacing-sm;
    font-size: 13px;
    color: $text-secondary;
  }
}

@keyframes dot-bounce {
  0%, 80%, 100% { transform: scale(0); opacity: 0.5; }
  40% { transform: scale(1); opacity: 1; }
}

.input-area {
  border-top: 1px solid $border-light;
  padding: $spacing-md;
}

.chat-input {
  display: flex;
  gap: $spacing-sm;

  .el-input { flex: 1; }
}

.judge-form-inline {
  display: flex;
  align-items: flex-start;
  gap: $spacing-md;

  .el-form { flex: 1; }

  :deep(.el-form-item) {
    margin-bottom: 0;
  }
}
</style>
