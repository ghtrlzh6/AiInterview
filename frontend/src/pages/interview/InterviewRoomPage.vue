<template>
  <div class="interview-room mx-auto flex w-full max-w-[1920px] flex-col">
    <!-- 顶部信息栏 -->
    <div class="flex items-center justify-between mb-3">
      <div>
        <h1 class="text-xl font-bold text-slate-800">{{ interview.positionName }}</h1>
        <p class="text-sm text-slate-500">
          会话 #{{ sessionId }}
          <span v-if="interview.currentQuestion">
            · 第 {{ interview.currentQuestion.questionOrder }} / {{ interview.totalQuestions }} 题
          </span>
          <span> · 用时 {{ elapsedTimeText }}</span>
        </p>
      </div>
      <div class="flex items-center gap-3">
        <ConnectionStatus :connected="interview.connectionOk" />
        <el-button type="danger" plain :disabled="interview.streaming" @click="handleEnd">
          结束面试
        </el-button>
      </div>
    </div>

    <!-- 手撕代码模式：全宽三列布局 -->
    <template v-if="isCodingQuestion">
      <div class="coding-workspace">
        <!-- 左侧：AI 对话 -->
        <section class="conversation-panel-coding flex flex-col gap-2">
          <div v-if="interview.currentQuestion" class="rounded-xl border bg-white p-3 shrink-0">
            <div class="flex flex-wrap items-center gap-2">
              <el-tag type="danger" effect="dark" size="small">手撕代码</el-tag>
              <el-tag v-if="interview.currentQuestion.topic" effect="plain" size="small">
                {{ interview.currentQuestion.topic }}
              </el-tag>
            </div>
            <div class="mt-1.5 font-medium text-slate-800 text-sm">
              {{ interview.currentQuestion.questionTitle }}
            </div>
          </div>

          <div ref="chatBox" class="flex-1 overflow-y-auto rounded-xl border bg-white p-4 space-y-3 min-h-0">
            <div
              v-for="(msg, i) in interview.messages"
              :key="i"
              class="flex"
              :class="msg.role === 'USER' ? 'justify-end' : 'justify-start'"
            >
              <div
                class="max-w-[90%] rounded-2xl px-3 py-2.5 text-sm whitespace-pre-wrap"
                :class="msg.role === 'USER' ? 'chat-bubble-user' : 'chat-bubble-assistant'"
              >
                <span v-html="renderMarkdown(msg.content)" />
                <span
                  v-if="interview.streaming && i === interview.messages.length - 1 && msg.role === 'ASSISTANT'"
                  class="inline-block w-2 h-4 bg-indigo-400 animate-pulse ml-1 align-middle"
                />
              </div>
            </div>
          </div>

          <div class="rounded-xl border bg-white p-3 shrink-0">
            <el-input
              v-model="inputText"
              type="textarea"
              :rows="2"
              placeholder="说明你的思路、时间复杂度或补充回答..."
              :disabled="interview.streaming"
              @keydown.ctrl.enter="send"
            />
            <div class="flex justify-between items-center mt-2">
              <span class="text-xs text-slate-400">Ctrl+Enter 发送</span>
              <el-button
                type="primary"
                size="small"
                :loading="interview.streaming"
                :disabled="!inputText.trim()"
                @click="send"
              >
                发送
              </el-button>
            </div>
          </div>
        </section>

        <!-- 右侧：LeetCode 风格代码面板 -->
        <CodingPanel
          :question="interview.currentQuestion"
          :session-id="sessionId"
          class="coding-ide-panel"
          @submitted="onCodeSubmitted"
        />
      </div>
    </template>

    <!-- 普通题目模式：原有布局 -->
    <template v-else>
      <div class="interview-workspace">
        <section class="conversation-panel">
          <div v-if="interview.currentQuestion" class="rounded-xl border bg-white p-4">
            <div class="flex flex-wrap items-center gap-2">
              <el-tag :type="questionTypeMeta.type" effect="dark">{{ questionTypeMeta.label }}</el-tag>
              <el-tag v-if="interview.currentQuestion.topic" effect="plain">
                {{ interview.currentQuestion.topic }}
              </el-tag>
            </div>
            <div class="mt-2 font-medium text-slate-800">{{ interview.currentQuestion.questionTitle }}</div>
          </div>

          <div ref="chatBox" class="chat-panel space-y-4 overflow-y-auto rounded-xl border bg-white p-5">
            <div
              v-for="(msg, i) in interview.messages"
              :key="i"
              class="flex"
              :class="msg.role === 'USER' ? 'justify-end' : 'justify-start'"
            >
              <div
                class="max-w-[85%] rounded-2xl px-4 py-3 text-sm whitespace-pre-wrap"
                :class="msg.role === 'USER' ? 'chat-bubble-user' : 'chat-bubble-assistant'"
              >
                <span v-html="renderMarkdown(msg.content)" />
                <span
                  v-if="interview.streaming && i === interview.messages.length - 1 && msg.role === 'ASSISTANT'"
                  class="inline-block w-2 h-4 bg-indigo-400 animate-pulse ml-1 align-middle"
                />
              </div>
            </div>
          </div>

          <div class="answer-panel rounded-xl border bg-white p-4">
            <VoiceInput
              v-if="interview.inputMode === 'VOICE'"
              :session-id="sessionId"
              :disabled="interview.streaming"
              @transcribed="onTranscribed"
            />
            <el-input
              v-model="inputText"
              type="textarea"
              :rows="3"
              placeholder="输入你的回答..."
              :disabled="interview.streaming"
              @keydown.ctrl.enter="send"
            />
            <div class="flex justify-end mt-3">
              <el-button
                type="primary"
                :loading="interview.streaming"
                :disabled="!inputText.trim()"
                @click="send"
              >
                发送（Ctrl+Enter）
              </el-button>
            </div>
          </div>
        </section>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { marked } from 'marked'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useInterviewStore } from '@/stores/interview'
import ConnectionStatus from '@/components/ConnectionStatus.vue'
import VoiceInput from '@/components/VoiceInput.vue'
import CodingPanel from '@/components/CodingPanel.vue'

const route    = useRoute()
const router   = useRouter()
const interview = useInterviewStore()
const inputText = ref('')
const chatBox = ref<HTMLElement | null>(null)
const elapsedSeconds = ref(0)
let timerId = 0

const sessionId = computed(() => Number(route.params.sessionId))
const elapsedTimeText = computed(() => formatElapsedTime(elapsedSeconds.value))

function renderMarkdown(text: string) {
  return marked.parse(text || '', { async: false }) as string
}

const questionTypeMeta = computed(() => {
  const type = interview.currentQuestion?.questionType
  const map = {
    SELF_INTRO:     { label: '自我介绍', type: 'info'    },
    TECH_KNOWLEDGE: { label: '技术基础', type: 'primary'  },
    SCENARIO:       { label: '场景设计', type: 'warning'  },
    PROJECT_DEEP:   { label: '项目深挖', type: 'success'  },
    BEHAVIOR:       { label: '手撕代码', type: 'danger'   },
  } as const
  return type ? map[type] : { label: '题目', type: 'info' as const }
})

const isCodingQuestion = computed(() => interview.currentQuestion?.questionType === 'BEHAVIOR')

function scrollBottom() {
  nextTick(() => {
    if (chatBox.value) chatBox.value.scrollTop = chatBox.value.scrollHeight
  })
}

function formatElapsedTime(totalSeconds: number) {
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60
  const mm = String(minutes).padStart(2, '0')
  const ss = String(seconds).padStart(2, '0')
  if (!hours) return `${mm}:${ss}`
  return `${String(hours).padStart(2, '0')}:${mm}:${ss}`
}

function getRoomStartedAt() {
  const key = `interview-room-started-at:${sessionId.value}`
  const saved = window.sessionStorage.getItem(key)
  if (saved) return Number(saved)

  const now = Date.now()
  window.sessionStorage.setItem(key, String(now))
  return now
}

function startRoomTimer() {
  const startedAt = getRoomStartedAt()
  const updateElapsed = () => {
    elapsedSeconds.value = Math.max(0, Math.floor((Date.now() - startedAt) / 1000))
  }

  updateElapsed()
  timerId = window.setInterval(updateElapsed, 1000)
}

watch(() => interview.messages.length, scrollBottom)
watch(() => interview.streamingContent, scrollBottom)

function onTranscribed(text: string) {
  const next = text.trim()
  if (!next) return
  inputText.value = inputText.value.trim() ? `${inputText.value}\n${next}` : next
}

function goEndPage() {
  const id = Number(route.params.sessionId) || sessionId.value
  if (!id || router.currentRoute.value.name === 'interview-end') return
  const query = interview.reportId ? { reportId: String(interview.reportId) } : undefined
  router.replace({ name: 'interview-end', params: { sessionId: String(id) }, query })
}

watch(() => interview.interviewEnded, (ended) => { if (ended) goEndPage() })

async function send() {
  if (!inputText.value.trim() || interview.streaming) return
  const text = inputText.value
  inputText.value = ''
  await interview.sendMessage(text, { onInterviewEnd: goEndPage })
  if (interview.interviewEnded) goEndPage()
}

function onCodeSubmitted(payload: { submitOrder: number; runStatus: string; testsPassed: number; testsTotal: number }) {
  const { runStatus, testsPassed, testsTotal } = payload
  if (runStatus === 'PASSED') {
    inputText.value = `代码通过了全部 ${testsTotal} 个测试用例。我使用的是……（请补充算法思路、时间/空间复杂度）`
  } else {
    inputText.value = `代码通过了 ${testsPassed}/${testsTotal} 个测试用例。我的思路是……（请补充）`
  }
}

async function handleEnd() {
  const action = await ElMessageBox.confirm(
    '结束后可以立即生成报告，也可以稍后在首页生成。',
    '结束面试',
    {
      confirmButtonText: '结束并生成报告',
      cancelButtonText: '暂不生成',
      distinguishCancelAndClose: true,
      type: 'warning',
    },
  )
    .then(() => 'generate')
    .catch((t) => (t === 'cancel' ? 'skip' : 'close'))
  if (action === 'close') return

  const res = await interview.end(action === 'generate')
  if (!res?.reportId) {
    ElMessage.success('面试已结束，可稍后在首页生成报告')
    router.push('/')
    return
  }
  router.push({
    name: 'interview-end',
    params: { sessionId: String(sessionId.value) },
    query: { reportId: String(res.reportId) },
  })
}

onMounted(async () => {
  startRoomTimer()
  if (interview.sessionId === sessionId.value && interview.messages.length) {
    if (interview.interviewEnded) goEndPage()
    return
  }
  try {
    const detail = await interview.restore(sessionId.value)
    if (detail.sessionStatus !== 'IN_PROGRESS') goEndPage()
  } catch {
    ElMessage.error('面试恢复失败，请重新选择岗位')
    router.replace('/interview/select')
  }
})

onUnmounted(() => {
  if (timerId) {
    window.clearInterval(timerId)
    timerId = 0
  }
})
</script>

<style scoped>
.interview-room {
  min-height: calc(100vh - 1rem);
}

/* =============================
   手撕代码专用布局（大屏左右分栏）
   ============================= */
.coding-workspace {
  display: flex;
  flex: 1;
  gap: 0.75rem;
  min-height: 0;
  height: calc(100vh - 5rem);
}

.conversation-panel-coding {
  width: 300px;
  min-width: 240px;
  flex-shrink: 0;
}

.coding-ide-panel {
  flex: 1;
  min-width: 0;
  height: 100%;
}

/* =============================
   普通题目布局
   ============================= */
.interview-workspace {
  display: grid;
  flex: 1;
  gap: 0.75rem;
  min-height: 0;
}

.conversation-panel {
  display: flex;
  min-height: 0;
  flex-direction: column;
  gap: 0.75rem;
}

.chat-panel {
  min-height: 520px;
}

@media (min-width: 1024px) {
  .interview-room {
    height: calc(100vh - 1.5rem);
  }
  .chat-panel {
    flex: 1;
    min-height: 0;
  }
}
</style>
