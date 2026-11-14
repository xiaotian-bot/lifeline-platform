import type { Router } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'
import { useAppStore } from '@/stores/app'
import { getAccessToken } from '@/utils/auth'

// 白名单路径
const WHITE_LIST = ['/login', '/403', '/404']

export function setupGuards(router: Router): void {
  router.beforeEach(async (to, _from, next) => {
    const token = getAccessToken()

    if (token) {
      if (to.path === '/login') {
        // 已登录，跳转首页
        next({ path: '/' })
      } else {
        const userStore = useUserStore()
        const permissionStore = usePermissionStore()
        const appStore = useAppStore()

        if (userStore.userInfo) {
          // 已有用户信息，直接放行
          appStore.addTag(to)
          next()
        } else {
          try {
            // 获取用户信息
            await userStore.fetchUserInfo()
            // 获取菜单并生成动态路由
            const menus = await permissionStore.fetchMenus()
            const dynamicRoutes = permissionStore.generateRoutes(menus)

            // 添加动态路由
            dynamicRoutes.forEach((route) => {
              router.addRoute('Layout', route)
            })

            // 添加404兜底路由（必须在动态路由之后）
            router.addRoute({
              path: '/:pathMatch(.*)*',
              redirect: '/404',
            })

            // 重新导航（确保动态路由已加载）
            next({ ...to, replace: true })
          } catch {
            // 获取用户信息失败，清除token并跳转登录
            userStore.logout()
            next(`/login?redirect=${to.path}`)
          }
        }
      }
    } else {
      // 未登录
      if (WHITE_LIST.includes(to.path)) {
        next()
      } else {
        next(`/login?redirect=${to.path}`)
      }
    }
  })
}
