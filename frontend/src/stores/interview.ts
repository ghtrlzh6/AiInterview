import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as interviewApi from '@/api/interview'
import { streamSse } from '@/utils/sse'
import type { ChatMessage, InterviewStartResult, SseEvent } from '@/types'

export const useInterviewStore = defineStore('interview', () => {
  const sessionId = ref<number | null>(null)
  const positionCode = ref('')
  const positionName = ref('')
  const totalQuestions = ref(0)
  const messages = ref<ChatMessage[]>([])
  const streaming = ref(false)
  const streamingContent = ref('')
  const inputMode = ref<'TEXT' | 'VOICE'>('TEXT')
  const reportId = ref<number | null>(null)
  const connectionOk = ref(true)

  let abortController: AbortController | null = null

  function reset() {
    sessionId.value = null
    positionCode.value = ''
    positionName.value = ''
    totalQuestions.value = 0
    messages.value = []
    streaming.value = false
    streamingContent.value = ''
    reportId.value = null
    abortController?.abort()
    abortController = null
  }

  function applyStartResult(res: InterviewStartResult) {
    sessionId.value = res.sessionId
    positionCode.value = res.positionCode
    positionName.value = res.positionName
    totalQuestions.value = res.totalQuestions
    messages.value = [res.firstMessage]
  }

  async function start(
    positionCodeVal: string,
    options?: { questionCount?: number; resumeSnapshotId?: number },
  ) {
    const res = await interviewApi.startInterview({
      positionCode: positionCodeVal,
      inputMode: inputMode.value,
      questionCount: options?.questionCount ?? 8,
      resumeSnapshotId: options?.resumeSnapshotId,
    })
    applyStartResult(res)
    return res
  }

  async function sendMessage(content: string) {
    if (!sessionId.value || !content.trim()) return

    messages.value.push({
      role: 'USER',
      content: content.trim(),
      messageType: 'NORMAL',
    })

    streaming.value = true
    streamingContent.value = ''
    connectionOk.value = true
    abortController = new AbortController()

    const assistantMsg: ChatMessage = { role: 'ASSISTANT', content: '' }
    messages.value.push(assistantMsg)
    const idx = messages.value.length - 1

    try {
      await streamSse({
        url: `/interviews/${sessionId.value}/message`,
        body: { content: content.trim(), messageType: 'NORMAL' },
        signal: abortController.signal,
        onEvent: (event: SseEvent) => {
          if (event.type === 'token' && event.content) {
            streamingContent.value += event.content
            messages.value[idx].content = streamingContent.value
          } else if (event.type === 'done') {
            streaming.value = false
            if (event.messageId) messages.value[idx].messageId = event.messageId
          } else if (event.type === 'next_question' && event.content) {
            streaming.value = false
            messages.value[idx].content = event.content
            messages.value.push({
              role: 'ASSISTANT',
              content: event.content,
              messageType: 'QUESTION',
              questionOrder: event.questionOrder,
            })
          } else if (event.type === 'interview_end') {
            streaming.value = false
            reportId.value = event.reportId ?? null
          } else if (event.type === 'error') {
            connectionOk.value = false
            messages.value[idx].content += `\n[错误] ${event.message || event.content || '未知错误'}`
          }
        },
        onError: () => {
          connectionOk.value = false
        },
      })
    } finally {
      streaming.value = false
      streamingContent.value = ''
    }
  }

  async function end() {
    if (!sessionId.value) return null
    const res = await interviewApi.endInterview(sessionId.value)
    reportId.value = res.reportId
    return res
  }

  return {
    sessionId,
    positionCode,
    positionName,
    totalQuestions,
    messages,
    streaming,
    streamingContent,
    inputMode,
    reportId,
    connectionOk,
    reset,
    start,
    sendMessage,
    end,
  }
})
