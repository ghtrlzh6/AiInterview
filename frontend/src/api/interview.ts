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
  return request.post<
    unknown,
    {
      submitId: number
      submitOrder: number
      questionId: number
      language: string
      review: string
      followUpSuggestion: string
      createdAt?: string
    }
  >(`/interviews/${sessionId}/coding-submit`, data)
}

export function getLatestCodingSubmit(sessionId: number, questionId: number) {
  return request.get<
    unknown,
    {
      submitted: boolean
      submitId?: number
      submitOrder?: number
      questionId?: number
      language?: string
      code?: string
      createdAt?: string
    }
  >(`/interviews/${sessionId}/coding-submit/latest`, { params: { questionId } })
}

export function convertAsr(formData: FormData) {
  return request.post<unknown, { text: string; duration: number; confidence: number }>(
    '/asr/convert',
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } },
  )
}
