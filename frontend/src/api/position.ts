import request from '@/utils/request'
import type { Position } from '@/types'

export function listPositions() {
  return request.get<unknown, Position[]>('/positions')
}

export function getPosition(code: string) {
  return request.get<unknown, Position>(`/positions/${code}`)
}
