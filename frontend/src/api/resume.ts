import request from '@/utils/request'
import type { ResumeProject, ResumeStatus } from '@/types'

export type ResumeParseStatus = 'PENDING' | 'SUCCESS' | 'FAILED'

export interface ResumeUploadResult {
  resumeId: number
  parseStatus: ResumeParseStatus
}

export interface ResumeProjectItem {
  id: number
  projectName: string
  summaryMd?: string
  techStackTokens?: string[]
}

export function uploadResume(fileOrForm: File | FormData) {
  const form = fileOrForm instanceof FormData ? fileOrForm : new FormData()
  if (fileOrForm instanceof File) {
    form.append('file', fileOrForm)
  }
  return request.post<unknown, ResumeUploadResult & ResumeStatus>('/resumes/upload', form)
}

export function getLatestResume() {
  return request.get<unknown, ResumeStatus>('/resumes/latest')
}

export function getResumeStatus(resumeId: number) {
  return request.get<unknown, ResumeStatus>(`/resumes/${resumeId}`)
}

export function getResumeProjects(resumeId: number) {
  return request.get<unknown, ResumeProjectItem[]>(`/resumes/${resumeId}/projects`)
}

export function listResumeProjects(resumeId: number) {
  return request.get<unknown, ResumeProject[]>(`/resumes/${resumeId}/projects`)
}

export async function waitForResumeParsed(
  resumeId: number,
  options?: { intervalMs?: number; maxAttempts?: number },
): Promise<ResumeStatus> {
  const intervalMs = options?.intervalMs ?? 1000
  const maxAttempts = options?.maxAttempts ?? 30
  for (let i = 0; i < maxAttempts; i++) {
    const status = await getResumeStatus(resumeId)
    if (status.parseStatus === 'SUCCESS' || status.parseStatus === 'FAILED') {
      return status
    }
    await new Promise((resolve) => setTimeout(resolve, intervalMs))
  }
  return getResumeStatus(resumeId)
}
