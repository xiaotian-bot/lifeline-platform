<template>
  <span class="status-tag" :style="{ backgroundColor: tagColor, color: textColor }">
    {{ label }}
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  level?: string    // blue/yellow/orange/red
  status?: string   // 工单状态
  label?: string
}>()

const levelColors: Record<string, string> = {
  blue: '#1E88E5',
  yellow: '#FDD835',
  orange: '#FB8C00',
  red: '#E53935',
}

const statusColors: Record<string, string> = {
  pending: '#909399',
  assigned: '#1E88E5',
  'in-progress': '#FB8C00',
  completed: '#43A047',
  closed: '#606266',
  rejected: '#E53935',
  normal: '#43A047',
  warning: '#FB8C00',
  fault: '#E53935',
  offline: '#909399',
  online: '#43A047',
  // 施工项目状态（数值）
  '0': '#909399',  // 待开工 info
  '1': '#1E88E5',  // 施工中 primary
  '2': '#43A047',  // 已完工 success
  '3': '#E53935',  // 已停工 danger
}

const tagColor = computed(() => {
  if (props.level && levelColors[props.level]) return levelColors[props.level]
  if (props.status && statusColors[props.status]) return statusColors[props.status]
  return '#909399'
})

const textColor = computed(() => {
  if (props.level === 'yellow') return '#333'
  return '#fff'
})
</script>

<style scoped lang="scss">
.status-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: $border-radius-sm;
  font-size: 12px;
  line-height: 1.5;
  white-space: nowrap;
}
</style>
