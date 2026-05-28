import request from '@/utils/request'
import type { GrowthData } from '@/types'

export function getGrowth(params?: { positionCode?: string; days?: number }) {
  return request.get<unknown, GrowthData>('/growth', { params })
}
