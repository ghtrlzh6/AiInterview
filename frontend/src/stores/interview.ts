import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as interviewApi from '@/api/interview'
import { streamSse } from '@/utils/sse'
import type {
  ActiveInterviewResult,
  ChatMessage,
  CurrentQuestion,
  InterviewSessionDetail,
  InterviewStartResult,
  SseEvent,
} from '@/types'

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
  const interviewEnded = ref(false)
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
    interviewEnded.value = false
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
    interviewEnded.value = false
  }

  function applySessionDetail(detail: InterviewSessionDetail, restoredMessages?: ChatMessage[]) {
    sessionId.value = detail.sessionId
    positionCode.value = detail.positionCode
    positionName.value = detail.positionName
    totalQuestions.value = detail.totalQuestions
    inputMode.value = detail.inputMode || inputMode.value
    currentQuestion.value = detail.currentQuestion ?? null
    messages.value = restoredMessages || messages.value
    reportId.value = null
    interviewEnded.value = detail.sessionStatus === 'COMPLETED'
    connectionOk.value = true
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

  async function restore(targetSessionId: number) {
    const [detail, messageResult] = await Promise.all([
      interviewApi.getInterview(targetSessionId),
      interviewApi.getInterviewMessages(targetSessionId),
    ])
    applySessionDetail(detail, messageResult.messages || [])
    return detail
  }

  async function loadActive() {
    const active = await interviewApi.getActiveInterview()
    if (!active.active || !active.sessionId) return null
    await restore(active.sessionId)
    return active as ActiveInterviewResult & { sessionId: number }
  }

  async function sendMessage(content: string, options?: { onInterviewEnd?: () => void }) {
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
          } else if (event.type === 'next_question') {
            // 面试官的自然反馈已通过 token 流式呈现，这里把"下一题"追加到同一条消息后面
            if (event.content) {
              streamingContent.value += event.content
              messages.value[idx].content = streamingContent.value
            }
            messages.value[idx].messageType = 'QUESTION'
            applyQuestionEvent(event)
          } else if (event.type === 'interview_end') {
            streaming.value = false
            // 结束语已通过 token 流式呈现，无需覆盖；仅标记结束并记录报告
            messages.value[idx].messageType = 'CLOSING'
            reportId.value = event.reportId ?? null
            interviewEnded.value = true
            options?.onInterviewEnd?.()
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

  async function end(generateReport = true) {
    if (!sessionId.value) return null
    const res = await interviewApi.endInterviewWithOptions(sessionId.value, { generateReport })
    reportId.value = res.reportId || null
    interviewEnded.value = true
    return res
  }

  async function generateReportForSession(targetSessionId: number) {
    const res = await interviewApi.generateReport(targetSessionId)
    reportId.value = res.reportId || null
    return res
  }

  async function submitCoding(language: string, code: string) {
    if (!sessionId.value || !currentQuestion.value) return null
    const res = await interviewApi.submitCoding(sessionId.value, {
      questionId: currentQuestion.value.questionId,
      language,
      code,
    })
    if (res && sessionId.value) await restore(sessionId.value)
    return res
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
    interviewEnded,
    connectionOk,
    currentQuestion,
    reset,
    start,
    restore,
    loadActive,
    sendMessage,
    end,
    generateReportForSession,
    submitCoding,
  }
})
