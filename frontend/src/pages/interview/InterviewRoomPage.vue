<template>
  <div class="max-w-4xl mx-auto flex flex-col h-[calc(100vh-8rem)]">
    <div class="flex items-center justify-between mb-4">
      <div>
        <h1 class="text-xl font-bold text-slate-800">{{ interview.positionName }}</h1>
        <p class="text-sm text-slate-500">会话 #{{ sessionId }}</p>
      </div>
      <div class="flex items-center gap-3">
        <ConnectionStatus :connected="interview.connectionOk" />
        <el-button type="danger" plain :disabled="interview.streaming" @click="handleEnd">
          结束面试
        </el-button>
      </div>
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
import { ElMessageBox } from 'element-plus'
import { useInterviewStore } from '@/stores/interview'
import ConnectionStatus from '@/components/ConnectionStatus.vue'
import VoiceInput from '@/components/VoiceInput.vue'

const route = useRoute()
const router = useRouter()
const interview = useInterviewStore()
const inputText = ref('')
const chatBox = ref<HTMLElement | null>(null)

const sessionId = computed(() => Number(route.params.sessionId))

function renderMarkdown(text: string) {
  return marked.parse(text || '', { async: false }) as string
}

function scrollBottom() {
  nextTick(() => {
    if (chatBox.value) chatBox.value.scrollTop = chatBox.value.scrollHeight
  })
}

watch(() => interview.messages.length, scrollBottom)
watch(() => interview.streamingContent, scrollBottom)

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
