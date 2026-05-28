<template>
  <div class="flex items-center gap-2">
    <el-button
      :type="recording ? 'danger' : 'primary'"
      circle
      :disabled="disabled"
      @click="toggleRecording"
    >
      <el-icon><Microphone /></el-icon>
    </el-button>
    <span v-if="recording" class="text-sm text-red-500 animate-pulse">录音中...</span>
    <span v-else-if="transcript" class="text-sm text-slate-600 flex-1 truncate">{{ transcript }}</span>
  </div>
</template>

<script setup lang="ts">
import { ref, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as interviewApi from '@/api/interview'

const props = defineProps<{
  sessionId?: number
  disabled?: boolean
}>()

const emit = defineEmits<{ transcribed: [text: string] }>()

const recording = ref(false)
const transcript = ref('')
let mediaRecorder: MediaRecorder | null = null
let chunks: Blob[] = []

async function toggleRecording() {
  if (recording.value) {
    stopRecording()
    return
  }
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    chunks = []
    mediaRecorder = new MediaRecorder(stream)
    mediaRecorder.ondataavailable = (e) => {
      if (e.data.size > 0) chunks.push(e.data)
    }
    mediaRecorder.onstop = async () => {
      stream.getTracks().forEach((t) => t.stop())
      const blob = new Blob(chunks, { type: 'audio/webm' })
      await uploadAudio(blob)
    }
    mediaRecorder.start()
    recording.value = true
  } catch {
    ElMessage.error('无法访问麦克风，请检查权限')
  }
}

function stopRecording() {
  mediaRecorder?.stop()
  recording.value = false
}

async function uploadAudio(blob: Blob) {
  const form = new FormData()
  form.append('audio', blob, 'recording.webm')
  if (props.sessionId) form.append('sessionId', String(props.sessionId))
  try {
    const res = await interviewApi.convertAsr(form)
    transcript.value = res.text
    emit('transcribed', res.text)
  } catch {
    ElMessage.error('语音识别失败')
  }
}

onUnmounted(() => {
  mediaRecorder?.stop()
})
</script>
