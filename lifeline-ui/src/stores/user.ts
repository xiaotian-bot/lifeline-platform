import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, refresh as refreshApi, getUserInfo as getUserInfoApi } from '@/api/auth'
import { getAccessToken, getRefreshToken, setTokens, clearTokens } from '@/utils/auth'
import type { LoginResponse, UserInfoResponse } from '@/types/auth.d'
import { useRouter } from 'vue-router'

export const useUserStore = defineStore('user', () => {
  const accessToken = ref<string | null>(getAccessToken())
  const refreshToken = ref<string | null>(getRefreshToken())
  const userInfo = ref<UserInfoResponse | null>(null)
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])

  const router = useRouter()

  /** 登录 */
  async function login(username: string, password: string): Promise<LoginResponse> {
    const data = await loginApi({ username, password })
    accessToken.value = data.accessToken
    refreshToken.value = data.refreshToken
    setTokens(data.accessToken, data.refreshToken)
    roles.value = data.roles || []
    return data
  }

  /** 刷新令牌 */
  async function refreshTokenAction(): Promise<void> {
    if (!refreshToken.value) {
      throw new Error('无刷新令牌')
    }
    const data = await refreshApi({ refreshToken: refreshToken.value })
    accessToken.value = data.accessToken
    refreshToken.value = data.refreshToken
    setTokens(data.accessToken, data.refreshToken)
  }

  /** 获取用户信息 */
  async function fetchUserInfo(): Promise<UserInfoResponse> {
    const data = await getUserInfoApi()
    userInfo.value = data
    roles.value = data.roles || []
    permissions.value = data.permissions || []
    return data
  }

  /** 退出登录 */
  function logout(): void {
    accessToken.value = null
    refreshToken.value = null
    userInfo.value = null
    roles.value = []
    permissions.value = []
    clearTokens()
    router.push('/login')
  }

  /** 是否已登录 */
  function isLoggedIn(): boolean {
    return !!accessToken.value
  }

  return {
    accessToken,
    refreshToken,
    userInfo,
    roles,
    permissions,
    login,
    refreshTokenAction,
    fetchUserInfo,
    logout,
    isLoggedIn,
  }
})
