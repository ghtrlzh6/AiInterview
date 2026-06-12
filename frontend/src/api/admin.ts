import request from '@/utils/request'
import type { AdminStats, PageResult, Position } from '@/types'

export function getAdminStats() {
  return request.get<unknown, AdminStats>('/admin/stats')
}

export function adminListPositions() {
  return request.get<unknown, Position[]>('/admin/positions')
}

export function adminCreatePosition(data: Partial<Position>) {
  return request.post('/admin/positions', data)
}

export function adminUpdatePosition(id: number, data: Partial<Position>) {
  return request.put(`/admin/positions/${id}`, data)
}

export function adminUpdatePositionStatus(id: number, enabled: boolean) {
  return request.put(`/admin/positions/${id}/status`, { enabled })
}

export function adminDeletePosition(id: number) {
  return request.delete(`/admin/positions/${id}`)
}

export function adminListQuestions(params?: Record<string, unknown>) {
  return request.get<unknown, PageResult<Record<string, unknown>>>('/admin/questions', {
    params,
  })
}

export function adminCreateQuestion(data: Record<string, unknown>) {
  return request.post('/admin/questions', data)
}

export function adminUpdateQuestion(id: number, data: Record<string, unknown>) {
  return request.put(`/admin/questions/${id}`, data)
}

export function adminDeleteQuestion(id: number) {
  return request.delete(`/admin/questions/${id}`)
}

export function adminBatchImportQuestions(data: unknown[]) {
  return request.post('/admin/questions/batch-import', data)
}

export function adminKbNodes(params?: { parentId?: number; keyword?: string }) {
  return request.get('/admin/kb/nodes', { params })
}

export function adminCreateKbNode(data: Record<string, unknown>) {
  return request.post('/admin/kb/nodes', data)
}

export function adminUpdateKbNode(nodeId: number, data: Record<string, unknown>) {
  return request.put(`/admin/kb/nodes/${nodeId}`, data)
}

export function adminDeleteKbNode(nodeId: number) {
  return request.delete(`/admin/kb/nodes/${nodeId}`)
}

export function adminCreateKbArticle(data: Record<string, unknown>) {
  return request.post('/admin/kb/articles', data)
}

export function adminUpdateKbArticle(articleId: number, data: Record<string, unknown>) {
  return request.put(`/admin/kb/articles/${articleId}`, data)
}

export function adminDeleteKbArticle(articleId: number) {
  return request.delete(`/admin/kb/articles/${articleId}`)
}

export function adminVectorizeArticle(articleId: number) {
  return request.post(`/admin/kb/articles/${articleId}/vectorize`)
}

export function adminVectorizePendingBatch() {
  return request.post('/admin/kb/vectorize-pending-batch')
}

export function adminGenerateQuestions(data: Record<string, unknown>) {
  return request.post('/admin/ai/questions/generate', data)
}

export interface AiConfigItem {
  key: string
  value: string
  type?: string
  sensitive?: boolean
  description?: string
}

export interface AiConfigTestResult {
  success: boolean
  model: string
  latencyMs: number
  message: string
}

export function adminGetAiConfig() {
  return request.get<unknown, AiConfigItem[]>('/admin/ai-config')
}

export function adminUpdateAiConfig(data: { key: string; value: string }[]) {
  return request.put('/admin/ai-config', data)
}

export function adminTestAiConfig() {
  return request.get<unknown, AiConfigTestResult>('/admin/ai-config/test')
}

export function adminListUsers(params?: { page?: number; size?: number; keyword?: string }) {
  return request.get<unknown, PageResult<Record<string, unknown>>>('/admin/users', { params })
}

export function adminGetUser(id: number) {
  return request.get(`/admin/users/${id}`)
}

export function adminUpdateUserRole(id: number, role: 'USER' | 'ADMIN') {
  return request.put(`/admin/users/${id}/role`, { role })
}

export function adminListResources(params?: Record<string, unknown>) {
  return request.get<unknown, PageResult<Record<string, unknown>>>('/admin/resources', { params })
}

export function adminCreateResource(data: Record<string, unknown>) {
  return request.post('/admin/resources', data)
}

export function adminUpdateResource(id: number, data: Record<string, unknown>) {
  return request.put(`/admin/resources/${id}`, data)
}

export function adminDeleteResource(id: number) {
  return request.delete(`/admin/resources/${id}`)
}
