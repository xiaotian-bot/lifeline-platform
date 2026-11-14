import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { setupGuards } from './guards'

// 静态路由
export const staticRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', hidden: true },
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/403.vue'),
    meta: { title: '无权限', hidden: true },
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在', hidden: true },
  },
  {
    path: '/dashboard/fullscreen',
    name: 'DashboardFullscreen',
    component: () => import('@/views/dashboard/fullscreen.vue'),
    meta: { title: '监测大屏', hidden: true, fullscreen: true },
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/layouts/default.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页', icon: 'HomeFilled', affix: true },
      },
      // 管网资产
      {
        path: 'asset/pipe',
        name: 'PipeList',
        component: () => import('@/views/asset/pipe/index.vue'),
        meta: { title: '管网台账' },
      },
      {
        path: 'asset/pipe/:id',
        name: 'PipeDetail',
        component: () => import('@/views/asset/pipe/detail.vue'),
        meta: { title: '管网详情', hidden: true },
      },
      {
        path: 'asset/map',
        name: 'AssetMap',
        component: () => import('@/views/asset/pipe/map.vue'),
        meta: { title: '管网GIS地图' },
      },
      {
        path: 'asset/manhole',
        name: 'ManholeList',
        component: () => import('@/views/asset/manhole/index.vue'),
        meta: { title: '井室管理' },
      },
      {
        path: 'asset/device',
        name: 'DeviceList',
        component: () => import('@/views/asset/device/index.vue'),
        meta: { title: '设备管理' },
      },
      // 预警中心
      {
        path: 'warning/center',
        name: 'WarningCenter',
        component: () => import('@/views/warning/center/index.vue'),
        meta: { title: '预警中心' },
      },
      {
        path: 'warning/rules',
        name: 'WarningRules',
        component: () => import('@/views/warning/rules/index.vue'),
        meta: { title: '预警规则' },
      },
      {
        path: 'warning/forecast',
        name: 'WarningForecast',
        component: () => import('@/views/warning/forecast/index.vue'),
        meta: { title: '超前预报' },
      },
      {
        path: 'warning/statistics',
        name: 'WarningStatistics',
        component: () => import('@/views/warning/statistics/index.vue'),
        meta: { title: '预警统计' },
      },
      // 施工监管
      {
        path: 'construction',
        name: 'ConstructionList',
        component: () => import('@/views/construction/index.vue'),
        meta: { title: '施工监管' },
      },
      {
        path: 'construction/:id',
        name: 'ConstructionDetail',
        component: () => import('@/views/construction/detail.vue'),
        meta: { title: '项目详情', hidden: true },
      },
      // 预案处置
      {
        path: 'plan/template',
        name: 'PlanTemplate',
        component: () => import('@/views/plan/template/index.vue'),
        meta: { title: '预案模板' },
      },
      {
        path: 'plan/instance',
        name: 'PlanInstance',
        component: () => import('@/views/plan/instance/index.vue'),
        meta: { title: '预案实例' },
      },
      {
        path: 'plan/instance/:id/tasks',
        name: 'PlanTasks',
        component: () => import('@/views/plan/instance/tasks.vue'),
        meta: { title: '任务进度', hidden: true },
      },
      // 系统管理
      {
        path: 'system/user',
        name: 'UserManage',
        component: () => import('@/views/system/user/index.vue'),
        meta: { title: '用户管理' },
      },
      {
        path: 'system/role',
        name: 'RoleManage',
        component: () => import('@/views/system/role/index.vue'),
        meta: { title: '角色管理' },
      },
      {
        path: 'system/log',
        name: 'OperationLog',
        component: () => import('@/views/system/log/index.vue'),
        meta: { title: '操作日志' },
      },
      // 区块链溯源
      {
        path: 'blockchain/trace',
        name: 'BlockchainTrace',
        component: () => import('@/views/blockchain/trace/index.vue'),
        meta: { title: '区块链溯源' },
      },
      // AI能力
      {
        path: 'ai/anomaly',
        name: 'AiAnomaly',
        component: () => import('@/views/ai/anomaly/index.vue'),
        meta: { title: '异常检测' },
      },
      {
        path: 'ai/forecast',
        name: 'AiForecast',
        component: () => import('@/views/ai/forecast/index.vue'),
        meta: { title: '趋势预报' },
      },
      {
        path: 'ai/agent',
        name: 'AiAgent',
        component: () => import('@/views/ai/agent/index.vue'),
        meta: { title: 'AI智能助手' },
      },
      {
        path: 'ai/vision',
        name: 'AiVision',
        component: () => import('@/views/ai/vision/index.vue'),
        meta: { title: '视觉识别' },
      },
      // 数字孪生
      {
        path: 'twin',
        name: 'DigitalTwin',
        component: () => import('@/views/twin/index.vue'),
        meta: { title: '数字孪生' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes: staticRoutes,
  scrollBehavior: () => ({ top: 0 }),
})

setupGuards(router)

export default router
