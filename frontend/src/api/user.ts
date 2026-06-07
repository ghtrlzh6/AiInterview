import request from '@/utils/request'
import type { PageResult, UserInfo } from '@/types'

export interface UserProfile extends UserInfo {
  email?: string
  school?: string
  major?: string
  educationExperience?: string
  personalSkills?: string
  projectExperience?: string
  internshipExperience?: string
  targetPositionName?: string
  totalInterviews?: number
  createdAt?: string
  role?: 'USER' | 'ADMIN'
}

export interface InterviewHistoryItem {
  sessionId: number
  positionCode: string
  positionName: string
  sessionStatus: string
  overallScore?: number
  durationSeconds?: number
  startTime: string
  endTime?: string
  reportId?: number
}

export function getMe() {
  return request.get<unknown, UserProfile>('/users/me')
}

export function updateMe(data: Partial<UserProfile>) {
  return request.put<unknown, UserProfile>('/users/me', data)
}

export function uploadAvatar(file: File) {
  const form = new FormData()
  form.append('file', file)
  return request.post<unknown, UserProfile>('/users/me/avatar', form)
}

export function getMyInterviews(params?: {
  page?: number
  size?: number
  positionCode?: string
}) {
  return request.get<unknown, PageResult<InterviewHistoryItem>>('/users/me/interviews', {
    params,
  })
}
