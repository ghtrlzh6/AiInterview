import request from '@/utils/request'
import type { ChatMessage, InterviewStartResult } from '@/types'

export interface StartInterviewPayload {
  positionCode: string
  inputMode?: 'TEXT' | 'VOICE'
  questionCount?: number
  resumeSnapshotId?: number
}

export function startInterview(data: StartInterviewPayload) {
  return request.post<unknown, InterviewStartResult>('/interviews/start', data)
}

export function endInterview(sessionId: number) {
  return request.post<
    unknown,
    { sessionId: number; reportId: number; reportStatus: string; message: string }
  >(`/interviews/${sessionId}/end`)
}

export function getInterview(sessionId: number) {
  return request.get<unknown, Record<string, unknown>>(`/interviews/${sessionId}`)
}

export function getInterviewMessages(sessionId: number) {
  return request.get<unknown, { sessionId: number; messages: ChatMessage[] }>(
    `/interviews/${sessionId}/messages`,
  )
}

export function submitCoding(
  sessionId: number,
  data: { questionId: number; language: string; code: string },
) {
  return request.post(`/interviews/${sessionId}/coding-submit`, data)
}

export function convertAsr(formData: FormData) {
  return request.post<unknown, { text: string; duration: number; confidence: number }>(
    '/asr/convert',
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } },
  )
}
