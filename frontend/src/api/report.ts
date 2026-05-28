import request from '@/utils/request'
import type { PageResult, ReportDetail } from '@/types'

export interface ReportListItem {
  reportId: number
  sessionId: number
  positionCode: string
  positionName: string
  reportStatus: string
  overallScore?: number
  createdAt: string
}

export function getReport(reportId: number) {
  return request.get<unknown, ReportDetail>(`/reports/${reportId}`)
}

export function listReports(params?: { page?: number; size?: number; positionCode?: string }) {
  return request.get<unknown, PageResult<ReportListItem>>('/reports', { params })
}

export function shareReport(reportId: number) {
  return request.post<unknown, { shareToken: string; shareUrl: string }>(
    `/reports/${reportId}/share`,
  )
}

export function getSharedReport(shareToken: string) {
  return request.get<unknown, ReportDetail>(`/reports/share/${shareToken}`)
}
