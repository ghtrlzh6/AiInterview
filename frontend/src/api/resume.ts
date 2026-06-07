import request from '@/utils/request'

export type ResumeParseStatus = 'PENDING' | 'SUCCESS' | 'FAILED'

export interface ResumeUploadResult {
  resumeId: number
  parseStatus: ResumeParseStatus
}

export interface ResumeStatus {
  resumeId?: number
  parseStatus?: ResumeParseStatus
  fileName?: string
  fileUrl?: string
  remark?: string
  createdAt?: string
  resumeTextPreview?: string
  parsedSections?: Record<string, string>
}

export interface ResumeProjectItem {
  id: number
  projectName: string
  summaryMd?: string
  techStackTokens?: string[]
}

export function uploadResume(file: File) {
  const form = new FormData()
  form.append('file', file)
  return request.post<unknown, ResumeUploadResult>('/resumes/upload', form)
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
