import request from '@/utils/request'
import type { LearningResource, PageResult } from '@/types'

export interface RecommendationItem {
  recommendationId: number
  resource: LearningResource
  reason?: string
}

export function getRecommendations(reportId: number) {
  return request.get<unknown, { reportId: number; recommendations: RecommendationItem[] }>(
    '/resources/recommendations',
    { params: { reportId } },
  )
}

export function feedbackRecommendation(recommendationId: number, isHelpful: boolean) {
  return request.post(`/resources/recommendations/${recommendationId}/feedback`, { isHelpful })
}

export function searchResources(params?: {
  positionCode?: string
  topic?: string
  type?: string
  page?: number
  size?: number
}) {
  return request.get<unknown, PageResult<LearningResource>>('/resources', { params })
}
