import { ref } from 'vue'

export function useFullscreen() {
  const isFullscreen = ref(false)

  function toggleFullscreen() {
    if (!document.fullscreenElement) {
      document.documentElement.requestFullscreen().then(() => {
        isFullscreen.value = true
      }).catch(() => {
        isFullscreen.value = false
      })
    } else {
      document.exitFullscreen().then(() => {
        isFullscreen.value = false
      }).catch(() => {
        isFullscreen.value = true
      })
    }
  }

  // 监听全屏变化
  document.addEventListener('fullscreenchange', () => {
    isFullscreen.value = !!document.fullscreenElement
  })

  return {
    isFullscreen,
    toggleFullscreen,
  }
}
