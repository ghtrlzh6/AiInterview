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
const useServerAsr = ref(false)

const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition
let recognition: any = null
let mediaRecorder: MediaRecorder | null = null
let audioChunks: Blob[] = []

function initRecognition() {
  if (!SpeechRecognition) {
    return null
  }

  const rec = new SpeechRecognition()
  rec.lang = 'zh-CN'
  rec.continuous = false
  rec.interimResults = false
  rec.maxAlternatives = 1

  rec.onresult = (event: any) => {
    const result = event.results[0][0].transcript
    transcript.value = result || '未识别到语音'
    emit('transcribed', transcript.value)
    recording.value = false
  }

  rec.onerror = (event: any) => {
    console.error('语音识别错误:', event.error)
    if (event.error === 'not-allowed') {
      ElMessage.error('麦克风权限被拒绝，请在浏览器设置中开启权限')
      recording.value = false
    } else if (event.error === 'no-speech') {
      fallbackToServerAsr()
    } else {
      fallbackToServerAsr()
    }
  }

  rec.onend = () => {
    if (recording.value && !useServerAsr.value) {
      rec.start()
    }
  }

  return rec
}

async function fallbackToServerAsr() {
  if (recognition) {
    try {
      recognition.stop()
    } catch {
      /* ignore */
    }
  }
  useServerAsr.value = true
  await startServerRecording()
}

async function startServerRecording() {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    audioChunks = []
    mediaRecorder = new MediaRecorder(stream)
    mediaRecorder.ondataavailable = (e) => {
      if (e.data.size > 0) audioChunks.push(e.data)
    }
    mediaRecorder.onstop = async () => {
      stream.getTracks().forEach((t) => t.stop())
      const blob = new Blob(audioChunks, { type: 'audio/webm' })
      await uploadAudio(blob)
    }
    mediaRecorder.start()
    recording.value = true
  } catch {
    ElMessage.error('无法访问麦克风，请检查权限设置')
    recording.value = false
    useServerAsr.value = false
  }
}

async function uploadAudio(blob: Blob) {
  try {
    const formData = new FormData()
    formData.append('audio', blob, 'recording.webm')
    formData.append('format', 'webm')
    if (props.sessionId) {
      formData.append('sessionId', String(props.sessionId))
    }
    const res = await interviewApi.convertAsr(formData)
    transcript.value = res.text || '未识别到语音'
    emit('transcribed', transcript.value)
    if (res.isMock) {
      ElMessage.info('当前使用 ASR 降级模式')
    }
  } catch {
    ElMessage.error('语音转写失败，请改用文字输入')
    transcript.value = '未识别到语音'
    emit('transcribed', transcript.value)
  } finally {
    recording.value = false
    useServerAsr.value = false
  }
}

async function toggleRecording() {
  if (recording.value) {
    stopRecording()
    return
  }

  transcript.value = ''
  useServerAsr.value = false

  if (SpeechRecognition) {
    try {
      await navigator.mediaDevices.getUserMedia({ audio: true })
      if (!recognition) {
        recognition = initRecognition()
      }
      if (recognition) {
        recording.value = true
        recognition.start()
        return
      }
    } catch {
      /* fallback below */
    }
  }

  await startServerRecording()
}

function stopRecording() {
  if (useServerAsr.value && mediaRecorder && mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop()
    return
  }
  if (recognition) {
    try {
      recognition.stop()
    } catch {
      /* ignore */
    }
  }
  recording.value = false
  useServerAsr.value = false
}

onUnmounted(() => {
  stopRecording()
  recognition = null
  mediaRecorder = null
})
</script>
