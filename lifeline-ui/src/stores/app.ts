import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { RouteLocationNormalized } from 'vue-router'

export interface TagView {
  path: string
  name: string
  title: string
  fullPath: string
  affix?: boolean
}

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const tagsViewList = ref<TagView[]>([])

  /** 切换侧边栏 */
  function toggleSidebar(): void {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  /** 添加标签 */
  function addTag(route: RouteLocationNormalized): void {
    const exists = tagsViewList.value.some((tag) => tag.path === route.path)
    if (!exists && route.name && route.meta?.title) {
      tagsViewList.value.push({
        path: route.path,
        name: route.name as string,
        title: route.meta.title as string,
        fullPath: route.fullPath,
      })
    }
  }

  /** 移除标签 */
  function removeTag(path: string): void {
    const index = tagsViewList.value.findIndex((tag) => tag.path === path)
    if (index > -1) {
      tagsViewList.value.splice(index, 1)
    }
  }

  /** 清空标签（保留固定标签） */
  function clearTags(): void {
    tagsViewList.value = tagsViewList.value.filter((tag) => tag.affix)
  }

  return {
    sidebarCollapsed,
    tagsViewList,
    toggleSidebar,
    addTag,
    removeTag,
    clearTags,
  }
})
