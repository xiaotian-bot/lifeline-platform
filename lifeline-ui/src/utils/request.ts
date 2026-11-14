import axios, { type AxiosInstance, type AxiosRequestConfig, type InternalAxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { getAccessToken, getRefreshToken, setTokens, clearTokens } from './auth'
import type { ApiResult } from '@/types/api.d'

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''
const NORMAL_TIMEOUT = 10000
const AI_TIMEOUT = 60000

// 创建 axios 实例
const service: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: NORMAL_TIMEOUT,
  headers: {
    'Content-Type': 'application/json',
  },
})

// ==================== Token 刷新状态 ====================
let isRefreshing = false
let pendingRequests: Array<(token: string) => void> = []

function onRefreshed(token: string) {
  pendingRequests.forEach((cb) => cb(token))
  pendingRequests = []
}

// ==================== 请求拦截器 ====================
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getAccessToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// ==================== 响应拦截器 ====================
service.interceptors.response.use(
  (response: AxiosResponse<ApiResult<unknown>>) => {
    const res = response.data

    // 业务层成功
    if (res.code === 200) {
      return res.data as unknown as AxiosResponse
    }

    // 401 / 20000: 需要刷新 token
    if (res.code === 401 || res.code === 20000) {
      return handleUnauthorized(response.config)
    }

    // 其他业务错误
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        return handleUnauthorized(error.config)
      }
      ElMessage.error(error.response.data?.message || `请求失败(${status})`)
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请稍后重试')
    } else {
      ElMessage.error('网络异常，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

// ==================== 处理 401 自动刷新 Token ====================
async function handleUnauthorized(config: AxiosRequestConfig): Promise<never> {
  const refreshToken = getRefreshToken()

  if (!refreshToken) {
    redirectToLogin()
    return Promise.reject(new Error('未登录'))
  }

  if (isRefreshing) {
    // 正在刷新，将请求加入队列
    return new Promise((resolve) => {
      pendingRequests.push((token: string) => {
        config.headers = config.headers || {}
        config.headers.Authorization = `Bearer ${token}`
        resolve(service(config) as never)
      })
    })
  }

  isRefreshing = true

  try {
    const { data } = await axios.post<ApiResult<{ accessToken: string; refreshToken: string }>>(
      `${BASE_URL}/api/v1/auth/refresh`,
      { refreshToken }
    )

    if (data.code === 200 && data.data) {
      const { accessToken: newAccess, refreshToken: newRefresh } = data.data
      setTokens(newAccess, newRefresh)
      onRefreshed(newAccess)

      // 重试原始请求
      config.headers = config.headers || {}
      config.headers.Authorization = `Bearer ${newAccess}`
      return service(config) as never
    } else {
      redirectToLogin()
      return Promise.reject(new Error('刷新令牌失败'))
    }
  } catch {
    redirectToLogin()
    return Promise.reject(new Error('刷新令牌失败'))
  } finally {
    isRefreshing = false
  }
}

function redirectToLogin() {
  clearTokens()
  window.location.href = '/login'
}

// ==================== 导出请求方法 ====================
export function get<T>(url: string, params?: Record<string, unknown>, config?: AxiosRequestConfig): Promise<T> {
  return service.get(url, { params, ...config }) as Promise<T>
}

export function post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return service.post(url, data, config) as Promise<T>
}

export function put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return service.put(url, data, config) as Promise<T>
}

export function del<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return service.delete(url, config) as Promise<T>
}

/** 创建带长超时的请求实例（AI/导出场景） */
export function createLongRequest(): AxiosInstance {
  const instance = axios.create({
    baseURL: BASE_URL,
    timeout: AI_TIMEOUT,
  })

  instance.interceptors.request.use((config: InternalAxiosRequestConfig) => {
    const token = getAccessToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  })

  return instance
}

export default service
