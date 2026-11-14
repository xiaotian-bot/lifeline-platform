import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getMenus } from '@/api/auth'
import type { MenuResponse } from '@/types/auth.d'
import type { RouteRecordRaw } from 'vue-router'

// 视图模块映射：将后端 path 映射到前端组件
const viewModules = import.meta.glob('@/views/**/*.vue')

export const usePermissionStore = defineStore('permission', () => {
  const menuTree = ref<MenuResponse[]>([])
  const buttonPermissions = ref<string[]>([])
  const routesGenerated = ref(false)

  /** 获取菜单树 */
  async function fetchMenus(): Promise<MenuResponse[]> {
    const data = await getMenus()
    menuTree.value = data
    // 提取所有按钮权限
    buttonPermissions.value = extractPermissions(data)
    return data
  }

  /** 递归提取权限标识 */
  function extractPermissions(menus: MenuResponse[]): string[] {
    const perms: string[] = []
    function walk(items: MenuResponse[]) {
      for (const item of items) {
        if (item.menuType === 'F' && item.permission) {
          perms.push(item.permission)
        }
        if (item.children?.length) {
          walk(item.children)
        }
      }
    }
    walk(menus)
    return perms
  }

  /** 根据菜单树生成动态路由 */
  function generateRoutes(menus: MenuResponse[]): RouteRecordRaw[] {
    const routes: RouteRecordRaw[] = []
    function walk(items: MenuResponse[], parentPath = '') {
      for (const item of items) {
        if (item.menuType === 'M' || item.menuType === 'C') {
          const cleanPath = item.path ? (item.path.startsWith('/') ? item.path.slice(1) : item.path) : ''
          const fullPath = cleanPath
            ? (parentPath ? `${parentPath}/${cleanPath}` : `/${cleanPath}`)
            : (parentPath || '/')

          // M 类型目录：仅递归处理子项，不创建路由（避免 404 父组件阻断子路由渲染）
          if (item.menuType === 'M') {
            if (item.children?.length) {
              walk(item.children, fullPath)
            }
            continue
          }

          const componentPath = `/src/views${fullPath}/index.vue`
          const component = viewModules[componentPath]

          const route: RouteRecordRaw = {
            path: fullPath,
            name: `dynamic-${fullPath.replace(/\//g, '-')}`,
            meta: {
              title: item.menuName,
              icon: item.icon,
              permission: item.permission,
            },
            component: component || (() => import('@/views/error/404.vue')),
            children: [],
          }

          if (item.children?.length) {
            const childRoutes: RouteRecordRaw[] = []
            walk(item.children, fullPath)
            childRoutes.forEach((child) => route.children!.push(child))
          }

          routes.push(route)
        }
      }
    }
    walk(menus)
    routesGenerated.value = true
    return routes
  }

  /** 重置 */
  function resetPermission(): void {
    menuTree.value = []
    buttonPermissions.value = []
    routesGenerated.value = false
  }

  return {
    menuTree,
    buttonPermissions,
    routesGenerated,
    fetchMenus,
    generateRoutes,
    resetPermission,
  }
})
