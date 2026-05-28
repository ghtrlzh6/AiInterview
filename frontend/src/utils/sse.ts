import { getAccessToken } from '@/utils/request'
import type { SseEvent } from '@/types'

export interface SseStreamOptions {
  url: string
  body?: Record<string, unknown>
  onEvent: (event: SseEvent) => void
  onError?: (err: Error) => void
  signal?: AbortSignal
}

export async function streamSse(options: SseStreamOptions): Promise<void> {
  const { url, body, onEvent, onError, signal } = options
  const token = getAccessToken()
  const base = import.meta.env.VITE_API_BASE_URL || '/api/v1'
  const fullUrl = url.startsWith('http') ? url : `${base}${url.replace(/^\/api\/v1/, '')}`

  const response = await fetch(fullUrl, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
    signal,
  })

  if (!response.ok) {
    const err = new Error(`SSE request failed: ${response.status}`)
    onError?.(err)
    throw err
  }

  const reader = response.body?.getReader()
  if (!reader) {
    throw new Error('No response body')
  }

  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })
    const lines = buffer.split('\n')
    buffer = lines.pop() || ''

    for (const line of lines) {
      const trimmed = line.trim()
      if (!trimmed || trimmed.startsWith(':')) continue
      if (trimmed.startsWith('data:')) {
        const jsonStr = trimmed.slice(5).trim()
        if (!jsonStr) continue
        try {
          const event = JSON.parse(jsonStr) as SseEvent
          onEvent(event)
        } catch {
          /* ignore malformed chunks */
        }
      }
    }
  }
}
