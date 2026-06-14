<template>
  <div class="interview-room mx-auto flex w-full max-w-[1680px] flex-col">
    <div class="flex items-center justify-between mb-3">
      <div>
        <h1 class="text-xl font-bold text-slate-800">{{ interview.positionName }}</h1>
        <p class="text-sm text-slate-500">
          会话 #{{ sessionId }}
          <span v-if="interview.currentQuestion">
            · 第 {{ interview.currentQuestion.questionOrder }} / {{ interview.totalQuestions }} 题
          </span>
        </p>
      </div>
      <div class="flex items-center gap-3">
        <ConnectionStatus :connected="interview.connectionOk" />
        <el-button type="danger" plain :disabled="interview.streaming" @click="handleEnd">
          结束面试
        </el-button>
      </div>
    </div>

    <div class="interview-workspace" :class="{ 'has-coding': isCodingQuestion }">
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

      <aside v-if="isCodingQuestion" class="coding-panel rounded-xl border bg-white p-4">
        <div class="mb-3 flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 class="text-base font-semibold text-slate-800">手撕代码提交</h2>
            <p class="text-sm text-slate-500">提交代码后，再用文字说明思路与复杂度。</p>
          </div>
          <el-select v-model="language" class="w-36">
            <el-option label="Java" value="java" />
            <el-option label="TypeScript" value="typescript" />
            <el-option label="Python" value="python" />
            <el-option label="C++" value="cpp" />
            <el-option label="C#" value="csharp" />
          </el-select>
        </div>
        <div class="mb-3 rounded border border-slate-200 bg-slate-50 p-3 text-sm text-slate-700">
          <div v-html="renderMarkdown(codingProblem)" />
        </div>
        <el-input
          v-model="codeText"
          type="textarea"
          :rows="10"
          resize="vertical"
          placeholder="在这里输入代码..."
          :disabled="submittingCode"
          class="code-input"
        />
        <div class="mt-3 flex flex-wrap items-center justify-between gap-3">
          <div class="text-sm text-slate-500">
            <span v-if="latestSubmitText">{{ latestSubmitText }}</span>
          </div>
          <el-button
            type="primary"
            :icon="CircleCheck"
            :loading="submittingCode"
            :disabled="!codeText.trim()"
            @click="submitCode"
          >
            提交代码
          </el-button>
        </div>
        <el-alert
          v-if="codeReview"
          class="mt-3"
          type="success"
          :closable="false"
          :title="codeReview"
        />
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { marked } from 'marked'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck } from '@element-plus/icons-vue'
import { useInterviewStore } from '@/stores/interview'
import * as interviewApi from '@/api/interview'
import ConnectionStatus from '@/components/ConnectionStatus.vue'
import VoiceInput from '@/components/VoiceInput.vue'

const route = useRoute()
const router = useRouter()
const interview = useInterviewStore()
const inputText = ref('')
const chatBox = ref<HTMLElement | null>(null)
const language = ref('java')
const codeText = ref('')
const codeReview = ref('')
const submittingCode = ref(false)
const latestSubmitText = ref('')

const sessionId = computed(() => Number(route.params.sessionId))

function renderMarkdown(text: string) {
  return marked.parse(text || '', { async: false }) as string
}

const questionTypeMeta = computed(() => {
  const type = interview.currentQuestion?.questionType
  const map = {
    SELF_INTRO: { label: '自我介绍', type: 'info' },
    TECH_KNOWLEDGE: { label: '技术基础', type: 'primary' },
    SCENARIO: { label: '场景设计', type: 'warning' },
    PROJECT_DEEP: { label: '项目深挖', type: 'success' },
    BEHAVIOR: { label: '手撕代码', type: 'danger' },
  } as const
  return type ? map[type] : { label: '题目', type: 'info' as const }
})

const isCodingQuestion = computed(() => interview.currentQuestion?.questionType === 'BEHAVIOR')

const codingProblem = computed(() => {
  const question = interview.currentQuestion
  return question?.codingChallenge?.problemMd || question?.questionTitle || ''
})

function scrollBottom() {
  nextTick(() => {
    if (chatBox.value) chatBox.value.scrollTop = chatBox.value.scrollHeight
  })
}

watch(() => interview.messages.length, scrollBottom)
watch(() => interview.streamingContent, scrollBottom)
watch(
  () => interview.currentQuestion?.questionId,
  async (questionId) => {
    codeReview.value = ''
    latestSubmitText.value = ''
    if (!questionId || !sessionId.value || !isCodingQuestion.value) {
      codeText.value = ''
      return
    }
    const latest = await interviewApi.getLatestCodingSubmit(sessionId.value, questionId)
    if (latest.submitted) {
      codeText.value = latest.code || ''
      language.value = latest.language || language.value
      latestSubmitText.value = `最近第 ${latest.submitOrder} 次提交已载入`
    }
  },
  { immediate: true },
)

function onTranscribed(text: string) {
  const current = inputText.value.trim()
  const next = text.trim()
  if (!next) return
  inputText.value = current ? `${current}\n${next}` : next
}

function goEndPage() {
  const currentSessionId = Number(route.params.sessionId) || sessionId.value
  if (!currentSessionId || router.currentRoute.value.name === 'interview-end') return
  const query = interview.reportId ? { reportId: String(interview.reportId) } : undefined
  router.replace({
    name: 'interview-end',
    params: { sessionId: String(currentSessionId) },
    query,
  })
}

watch(
  () => interview.interviewEnded,
  (ended) => {
    if (ended) goEndPage()
  },
)

async function send() {
  if (!inputText.value.trim() || interview.streaming) return
  const text = inputText.value
  inputText.value = ''
  await interview.sendMessage(text, { onInterviewEnd: goEndPage })
  if (interview.interviewEnded) goEndPage()
}

async function submitCode() {
  if (!codeText.value.trim()) return
  submittingCode.value = true
  try {
    const res = await interview.submitCoding(language.value, codeText.value)
    if (res) {
      codeReview.value = res.followUpSuggestion || res.message || '代码已同步到左侧对话'
      latestSubmitText.value = `第 ${res.submitOrder} 次提交成功`
      ElMessage.success('代码已提交')
    }
  } finally {
    submittingCode.value = false
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
    .catch((actionType) => (actionType === 'cancel' ? 'skip' : 'close'))
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
  if (interview.sessionId === sessionId.value && interview.messages.length) {
    if (interview.interviewEnded) goEndPage()
    return
  }
  try {
    const detail = await interview.restore(sessionId.value)
    if (detail.sessionStatus !== 'IN_PROGRESS') {
      goEndPage()
    }
  } catch {
    ElMessage.error('面试恢复失败，请重新选择岗位')
    router.replace('/interview/select')
  }
})

onUnmounted(() => {
  /* keep session for end page */
})
</script>

<style scoped>
.interview-room {
  min-height: calc(100vh - 1rem);
}

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

.code-input :deep(textarea) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', monospace;
}

@media (min-width: 1024px) {
  .interview-room {
    height: calc(100vh - 1.5rem);
  }

  .interview-workspace.has-coding {
    grid-template-columns: minmax(0, 1fr) minmax(400px, 500px);
  }

  .chat-panel {
    flex: 1;
    min-height: 0;
  }

  .coding-panel {
    min-height: 0;
    overflow-y: auto;
  }
}
</style>
