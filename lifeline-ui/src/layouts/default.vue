<template>
  <div class="layout-container">
    <!-- 侧边栏 -->
    <div class="sidebar" :class="{ 'is-collapsed': appStore.sidebarCollapsed }">
      <div class="logo">
        <span v-if="!appStore.sidebarCollapsed" class="logo-text">生命线智慧平台</span>
        <span v-else class="logo-text-mini">线</span>
      </div>
      <el-scrollbar>
        <el-menu
          :default-active="activeMenu"
          :collapse="appStore.sidebarCollapsed"
          :collapse-transition="false"
          background-color="#001529"
          text-color="#BFCBD9"
          active-text-color="#FFFFFF"
          router
        >
          <template v-for="route in menuRoutes" :key="route.path">
            <!-- 单级菜单 -->
            <el-menu-item v-if="!route.children || route.children.length === 0" :index="route.path">
              <el-icon v-if="route.meta?.icon">
                <component :is="route.meta.icon" />
              </el-icon>
              <template #title>{{ route.meta?.title }}</template>
            </el-menu-item>

            <!-- 多级菜单（目录 index 加 -dir 后缀，避免与唯一子项 index 同键冲突） -->
            <el-sub-menu v-else :index="`${route.path}-dir`">
              <template #title>
                <el-icon v-if="route.meta?.icon">
                  <component :is="route.meta.icon" />
                </el-icon>
                <span>{{ route.meta?.title }}</span>
              </template>
              <el-menu-item
                v-for="child in route.children"
                :key="child.path"
                :index="child.path"
              >
                {{ child.meta?.title }}
              </el-menu-item>
            </el-sub-menu>
          </template>
        </el-menu>
      </el-scrollbar>
    </div>

    <!-- 右侧内容区 -->
    <div class="main-area">
      <!-- 头部 -->
      <div class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="appStore.toggleSidebar">
            <Fold v-if="!appStore.sidebarCollapsed" />
            <Expand v-else />
          </el-icon>
          <!-- 面包屑 -->
          <el-breadcrumb separator="/">
            <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
              {{ item.meta?.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <!-- 全屏 -->
          <el-tooltip content="全屏" placement="bottom">
            <el-icon class="header-icon" @click="toggleFullscreen">
              <FullScreen />
            </el-icon>
          </el-tooltip>
          <!-- 预警通知 -->
          <el-badge :value="warningStore.unreadCount" :hidden="warningStore.unreadCount === 0" class="notification-badge">
            <el-icon class="header-icon">
              <Bell />
            </el-icon>
          </el-badge>
          <!-- 用户菜单 -->
          <el-dropdown trigger="click" @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="28" class="user-avatar">
                {{ userStore.userInfo?.realName?.charAt(0) || 'U' }}
              </el-avatar>
              <span class="user-name">{{ userStore.userInfo?.realName || userStore.userInfo?.username || '用户' }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <!-- 标签栏 -->
      <div class="tags-view">
        <el-tag
          v-for="tag in appStore.tagsViewList"
          :key="tag.path"
          :closable="!tag.affix"
          :type="tag.path === currentPath ? '' : 'info'"
          class="tag-item"
          @click="navigateTo(tag.fullPath)"
          @close="appStore.removeTag(tag.path)"
        >
          {{ tag.title }}
        </el-tag>
      </div>

      <!-- 主内容 -->
      <div class="content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Fold, Expand, FullScreen, Bell } from '@element-plus/icons-vue'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { useWarningStore } from '@/stores/warning'
import { useFullscreen } from '@/composables/useFullscreen'
import { staticRoutes } from '@/router'
import { usePermissionStore } from '@/stores/permission'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()
const permissionStore = usePermissionStore()
const warningStore = useWarningStore()
const { toggleFullscreen } = useFullscreen()

const activeMenu = computed(() => route.path)
const currentPath = computed(() => route.path)

const breadcrumbs = computed(() => {
  return route.matched.filter((item) => item.meta?.title)
})

const menuRoutes = computed(() => {
  // 合并静态路由（Dashboard）和动态路由
  const layoutRoute = staticRoutes.find((r) => r.path === '/')
  const dashboardRoute = layoutRoute?.children?.[0]
  const result = []
  if (dashboardRoute) {
    result.push({ ...dashboardRoute, path: '/dashboard' })
  }
  // 动态路由从 permissionStore 获取（仅保留 M 目录/C 菜单，过滤异常提升到顶层的 F 按钮节点）
  const menus = permissionStore.menuTree.filter((m) => m.menuType === 'M' || m.menuType === 'C')
  for (const menu of menus) {
    const fullPath = menu.path.startsWith('/') ? menu.path : `/${menu.path}`
    const childMenus = (menu.children || []).filter((c: any) => c.menuType === 'C')
    if (childMenus.length > 0) {
      result.push({
        path: fullPath,
        meta: { title: menu.menuName, icon: menu.icon },
        children: childMenus.map((child: any) => {
          // 生成与动态路由一致的完整路径：非空时拼接父目录路径，为空时直接用父目录路径
          // 与 permission.ts 的路径归一化规则保持一致：只去掉一个前导斜杠（等价 item.path.slice(1)）
          const cleanChildPath = child.path
            ? (String(child.path).startsWith('/') ? String(child.path).slice(1) : String(child.path))
            : ''
          const childFullPath = cleanChildPath ? `${fullPath}/${cleanChildPath}` : fullPath
          return {
            path: childFullPath,
            meta: { title: child.menuName, icon: child.icon },
          }
        }),
      })
    } else {
      result.push({
        path: fullPath,
        meta: { title: menu.menuName, icon: menu.icon },
      })
    }
  }
  return result
})

function handleCommand(command: string) {
  if (command === 'logout') {
    userStore.logout()
  }
}

function navigateTo(path: string) {
  router.push(path)
}
</script>

<style scoped lang="scss">
.layout-container {
  display: flex;
  width: 100%;
  height: 100%;
}

.sidebar {
  width: $sidebar-width;
  height: 100%;
  background-color: $sidebar-bg;
  transition: width 0.3s ease;
  overflow: hidden;
  flex-shrink: 0;

  &.is-collapsed {
    width: $sidebar-collapsed-width;
  }
}

.logo {
  height: $header-height;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo-text {
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
}

.logo-text-mini {
  color: #fff;
  font-size: 18px;
  font-weight: 700;
}

.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

.header {
  height: $header-height;
  background: $header-bg;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 $spacing-md;
  box-shadow: $shadow-light;
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
  gap: $spacing-md;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: $text-regular;

  &:hover {
    color: $primary-color;
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: $spacing-lg;
}

.header-icon {
  font-size: 18px;
  cursor: pointer;
  color: $text-regular;

  &:hover {
    color: $primary-color;
  }
}

.user-info {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  cursor: pointer;
}

.user-avatar {
  background-color: $primary-color;
  color: #fff;
}

.user-name {
  font-size: 14px;
  color: $text-primary;
}

.tags-view {
  height: $tags-view-height;
  display: flex;
  align-items: center;
  padding: 0 $spacing-sm;
  background: #fff;
  border-bottom: 1px solid $border-lighter;
  gap: $spacing-xs;
  overflow-x: auto;

  .tag-item {
    cursor: pointer;
    flex-shrink: 0;
  }
}

.content {
  flex: 1;
  padding: $spacing-md;
  overflow-y: auto;
  background-color: $bg-color-page;
}

.notification-badge {
  display: flex;
  align-items: center;
}
</style>
