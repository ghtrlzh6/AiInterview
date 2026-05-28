import request from '@/utils/request'
import type { LoginResult, UserInfo } from '@/types'

export interface RegisterPayload {
  username: string
  password: string
  nickname: string
  email?: string
}

export interface LoginPayload {
  username: string
  password: string
}

export function register(data: RegisterPayload) {
  return request.post<unknown, UserInfo & { userId: number }>('/auth/register', data)
}

export function login(data: LoginPayload) {
  return request.post<unknown, LoginResult>('/auth/login', data)
}

export function refreshToken(refreshToken: string) {
  return request.post<unknown, { accessToken: string; expiresIn: number }>('/auth/refresh', {
    refreshToken,
  })
}

export function logout() {
  return request.post('/auth/logout')
}
