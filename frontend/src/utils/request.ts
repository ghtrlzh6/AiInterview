import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/types'

const TOKEN_KEY = 'accessToken'
const REFRESH_KEY = 'refreshToken'

export function getAccessToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setTokens(access: string, refresh?: string): void {
  localStorage.setItem(TOKEN_KEY, access)
  if (refresh) localStorage.setItem(REFRESH_KEY, refresh)
}

export function clearTokens(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_KEY)
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_KEY)
}

const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 60000,
})

request.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse
    if (body && typeof body.code === 'number') {
      if (body.code === 200) {
        return body.data as never
      }
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message))
    }
    return response.data as never
  },
  (error) => {
    const msg = error.response?.data?.message || error.message || '网络错误'
    if (error.response?.status === 401) {
      clearTokens()
      if (!window.location.pathname.includes('/login')) {
        window.location.href = '/login'
      }
    } else {
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  },
)

export default request
