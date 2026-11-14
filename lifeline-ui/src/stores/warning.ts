import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useWarningStore = defineStore('warning', () => {
  const warningCount = ref(0)
  const unreadCount = ref(0)

  function setWarningCount(count: number): void {
    warningCount.value = count
  }

  function setUnreadCount(count: number): void {
    unreadCount.value = count
  }

  function incrementWarning(): void {
    warningCount.value++
  }

  function reset(): void {
    warningCount.value = 0
    unreadCount.value = 0
  }

  return {
    warningCount,
    unreadCount,
    setWarningCount,
    setUnreadCount,
    incrementWarning,
    reset,
  }
})
