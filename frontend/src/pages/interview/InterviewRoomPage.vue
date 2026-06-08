<template>
  <div class="max-w-4xl mx-auto flex flex-col h-[calc(100vh-8rem)]">
    <div class="flex items-center justify-between mb-4">
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

    <div v-if="interview.currentQuestion" class="mb-4 rounded-xl border bg-white p-4">
      <div class="flex flex-wrap items-center gap-2">
        <el-tag :type="questionTypeMeta.type" effect="dark">{{ questionTypeMeta.label }}</el-tag>
        <el-tag v-if="interview.currentQuestion.topic" effect="plain">
          {{ interview.currentQuestion.topic }}
        </el-tag>
      </div>
      <div class="mt-2 font-medium text-slate-800">{{ interview.currentQuestion.questionTitle }}</div>
    </div>

    <div ref="chatBox" class="flex-1 overflow-y-auto space-y-4 p-4 bg-white rounded-xl border mb-4">
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

    <div v-if="isBehaviorQuestion" class="mb-4 rounded-xl border bg-white p-4">
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
        :rows="8"
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
    </div>

    <div class="bg-white rounded-xl border p-4">
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
          发送 (Ctrl+Enter)
        </el-button>
      </div>
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
    TECH_KNOWLEDGE: { label: '技术基础', type: 'primary' },
    SCENARIO: { label: '场景设计', type: 'warning' },
    PROJECT_DEEP: { label: '项目深挖', type: 'success' },
    BEHAVIOR: { label: '手撕代码', type: 'danger' },
  } as const
  return type ? map[type] : { label: '题目', type: 'info' as const }
})

const isBehaviorQuestion = computed(() => interview.currentQuestion?.questionType === 'BEHAVIOR')

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
    if (!questionId || !sessionId.value || !isBehaviorQuestion.value) {
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
  inputText.value = text
}

async function send() {
  if (!inputText.value.trim() || interview.streaming) return
  const text = inputText.value
  inputText.value = ''
  await interview.sendMessage(text)
  if (interview.reportId) {
    router.push({
      name: 'interview-end',
      params: { sessionId: String(sessionId.value) },
      query: { reportId: String(interview.reportId) },
    })
  }
}

async function submitCode() {
  if (!codeText.value.trim()) return
  submittingCode.value = true
  try {
    const res = await interview.submitCoding(language.value, codeText.value)
    if (res) {
      codeReview.value = res.review
      latestSubmitText.value = `第 ${res.submitOrder} 次提交成功`
      ElMessage.success('代码已提交')
    }
  } finally {
    submittingCode.value = false
  }
}

async function handleEnd() {
  await ElMessageBox.confirm('确定要结束本次面试吗？', '结束面试')
  const res = await interview.end()
  router.push({
    name: 'interview-end',
    params: { sessionId: String(sessionId.value) },
    query: { reportId: String(res?.reportId || '') },
  })
}

onMounted(() => {
  if (!interview.sessionId || interview.sessionId !== sessionId.value) {
    if (!interview.messages.length) {
      router.replace('/interview/select')
    }
  }
})

onUnmounted(() => {
  /* keep session for end page */
})
</script>

<style scoped>
.code-input :deep(textarea) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', monospace;
}
</style>
