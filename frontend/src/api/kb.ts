import request from '@/utils/request'
import type { KbArticle, KbNodeDetail, KbTreeNode } from '@/types'

export function getKbTree(params?: { parentId?: number; positionCode?: string }) {
  return request.get<unknown, KbTreeNode[]>('/kb/tree', { params })
}

export function getKbNode(nodeId: number) {
  return request.get<unknown, KbNodeDetail>(`/kb/nodes/${nodeId}`)
}

export function getKbArticle(articleId: number) {
  return request.get<unknown, KbArticle & { bodyMarkdown: string }>(`/kb/articles/${articleId}`)
}
