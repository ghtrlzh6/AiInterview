import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as authApi from '@/api/auth'
import * as userApi from '@/api/user'
import {
  clearTokens,
  getAccessToken,
  getRefreshToken,
  setTokens,
} from '@/utils/request'
import type { LoginPayload, RegisterPayload } from '@/api/auth'
import type { UserInfo, UserRole } from '@/types'

const USER_KEY = 'userInfo'

export const useAuthStore = defineStore('auth', () => {
  const userInfo = ref<UserInfo | null>(loadUser())
  const loading = ref(false)

  const isLoggedIn = computed(() => !!getAccessToken())
  const isAdmin = computed(() => userInfo.value?.role === 'ADMIN')

  function loadUser(): UserInfo | null {
    const raw = localStorage.getItem(USER_KEY)
    if (!raw) return null
    try {
      return JSON.parse(raw) as UserInfo
    } catch {
      return null
    }
  }

  function persistUser(user: UserInfo | null) {
    if (user) {
      localStorage.setItem(USER_KEY, JSON.stringify(user))
    } else {
      localStorage.removeItem(USER_KEY)
    }
    userInfo.value = user
  }

  async function login(payload: LoginPayload) {
    loading.value = true
    try {
      const res = await authApi.login(payload)
      setTokens(res.accessToken, res.refreshToken)
      persistUser(res.userInfo)
      await fetchProfile()
      return res
    } finally {
      loading.value = false
    }
  }

  async function register(payload: RegisterPayload) {
    loading.value = true
    try {
      await authApi.register(payload)
      return login({ username: payload.username, password: payload.password })
    } finally {
      loading.value = false
    }
  }

  async function fetchProfile() {
    if (!getAccessToken()) return
    try {
      const profile = await userApi.getMe()
      persistUser({
        userId: profile.userId,
        username: profile.username,
        nickname: profile.nickname,
        avatarUrl: profile.avatarUrl,
        targetPositionCode: profile.targetPositionCode,
        role: (profile.role as UserRole) || userInfo.value?.role || 'USER',
      })
    } catch {
      /* profile optional on boot */
    }
  }

  async function logout() {
    try {
      await authApi.logout()
    } catch {
      /* ignore */
    }
    clearTokens()
    persistUser(null)
  }

  async function tryRefresh(): Promise<boolean> {
    const refresh = getRefreshToken()
    if (!refresh) return false
    try {
      const res = await authApi.refreshToken(refresh)
      setTokens(res.accessToken)
      return true
    } catch {
      clearTokens()
      persistUser(null)
      return false
    }
  }

  function init() {
    if (getAccessToken()) {
      fetchProfile()
    }
  }

  return {
    userInfo,
    loading,
    isLoggedIn,
    isAdmin,
    login,
    register,
    logout,
    fetchProfile,
    tryRefresh,
    init,
  }
})
