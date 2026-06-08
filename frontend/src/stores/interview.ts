import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as interviewApi from '@/api/interview'
import { streamSse } from '@/utils/sse'
import type { ChatMessage, CurrentQuestion, InterviewStartResult, SseEvent } from '@/types'

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
  const currentQuestion = ref<CurrentQuestion | null>(null)

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
    currentQuestion.value = null
    abortController?.abort()
    abortController = null
  }

  function applyStartResult(res: InterviewStartResult) {
    sessionId.value = res.sessionId
    positionCode.value = res.positionCode
    positionName.value = res.positionName
    totalQuestions.value = res.totalQuestions
    messages.value = [res.firstMessage]
    currentQuestion.value = res.currentQuestion ?? questionFromMessage(res.firstMessage)
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
      questionId: currentQuestion.value?.questionId,
      questionOrder: currentQuestion.value?.questionOrder,
      questionType: currentQuestion.value?.questionType,
      questionTitle: currentQuestion.value?.questionTitle,
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
            applyQuestionEvent(event)
          } else if (event.type === 'next_question' && event.content) {
            streaming.value = false
            messages.value[idx].content = event.content
            messages.value[idx].messageType = 'QUESTION'
            applyQuestionEvent(event)
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

  async function submitCoding(language: string, code: string) {
    if (!sessionId.value || !currentQuestion.value) return null
    return interviewApi.submitCoding(sessionId.value, {
      questionId: currentQuestion.value.questionId,
      language,
      code,
    })
  }

  function applyQuestionEvent(event: SseEvent) {
    if (!event.questionId || !event.questionOrder || !event.questionType || !event.questionTitle) return
    currentQuestion.value = {
      questionId: event.questionId,
      questionOrder: event.questionOrder,
      questionType: event.questionType,
      questionTitle: event.questionTitle,
      topic: event.topic,
      codingChallenge: event.codingChallenge,
    }
  }

  function questionFromMessage(message: ChatMessage): CurrentQuestion | null {
    if (!message.questionId || !message.questionOrder || !message.questionType || !message.questionTitle) {
      return null
    }
    return {
      questionId: message.questionId,
      questionOrder: message.questionOrder,
      questionType: message.questionType,
      questionTitle: message.questionTitle,
      topic: message.topic,
    }
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
    currentQuestion,
    reset,
    start,
    sendMessage,
    end,
    submitCoding,
  }
})
