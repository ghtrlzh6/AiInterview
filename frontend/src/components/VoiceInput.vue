<template>
  <div class="flex flex-wrap items-center gap-3">
    <el-button
      :type="recording ? 'danger' : 'primary'"
      :loading="transcribing"
      :disabled="disabled"
      @click="toggleRecording"
    >
      <el-icon><Microphone /></el-icon>
      <span>{{ recording ? '停止并转写' : '开始语音输入' }}</span>
    </el-button>
    <div v-if="recording" class="voice-wave" aria-label="录音音量">
      <span
        v-for="(height, index) in waveHeights"
        :key="index"
        class="voice-wave__bar"
        :style="{ height: `${height}px` }"
      />
    </div>
    <span v-if="recording" class="text-sm text-red-500">录音中，点击按钮结束</span>
    <span v-else-if="transcribing" class="text-sm text-slate-500">正在转写...</span>
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
const transcribing = ref(false)
const transcript = ref('')
const useServerAsr = ref(false)
const waveHeights = ref([8, 12, 18, 14, 10, 16, 12, 8])

const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition
let recognition: any = null
let mediaRecorder: MediaRecorder | null = null
let audioStream: MediaStream | null = null
let audioContext: AudioContext | null = null
let analyser: AnalyserNode | null = null
let animationFrame = 0
let audioChunks: Blob[] = []
let finalTranscript = ''
let stoppingByUser = false
let restartingRecognition = false
let shouldUploadRecording = true

function getDisplayText(interimText = '') {
  return `${finalTranscript} ${interimText}`.trim()
}

function updateTranscript(text: string) {
  const value = text.trim()
  transcript.value = value
}

function emitTranscript(text: string) {
  const value = text.trim()
  if (value) {
    emit('transcribed', value)
  }
}

function resetTranscript() {
  finalTranscript = ''
  transcript.value = ''
}

function stopAudioMonitor() {
  if (animationFrame) {
    cancelAnimationFrame(animationFrame)
    animationFrame = 0
  }
  if (audioContext) {
    void audioContext.close()
    audioContext = null
  }
  analyser = null
  if (audioStream) {
    audioStream.getTracks().forEach((track) => track.stop())
    audioStream = null
  }
  waveHeights.value = [8, 12, 18, 14, 10, 16, 12, 8]
}

function startAudioMonitor(stream: MediaStream) {
  stopAudioMonitor()
  audioStream = stream
  const AudioContextCtor = window.AudioContext || (window as any).webkitAudioContext
  if (!AudioContextCtor) return

  audioContext = new AudioContextCtor()
  analyser = audioContext.createAnalyser()
  analyser.fftSize = 256
  const source = audioContext.createMediaStreamSource(stream)
  source.connect(analyser)

  const data = new Uint8Array(analyser.frequencyBinCount)
  const tick = () => {
    if (!analyser) return
    analyser.getByteFrequencyData(data)
    const average = data.reduce((sum, value) => sum + value, 0) / data.length
    const level = Math.min(1, average / 90)
    waveHeights.value = waveHeights.value.map((_, index) => {
      const phase = Math.sin(Date.now() / 140 + index * 0.85)
      const movement = Math.max(0.18, level) * (18 + phase * 8)
      return Math.round(6 + movement)
    })
    animationFrame = requestAnimationFrame(tick)
  }
  tick()
}

function initRecognition() {
  if (!SpeechRecognition) {
    return null
  }

  const rec = new SpeechRecognition()
  rec.lang = 'zh-CN'
  rec.continuous = true
  rec.interimResults = true
  rec.maxAlternatives = 1

  rec.onresult = (event: any) => {
    let interimTranscript = ''
    for (let i = event.resultIndex; i < event.results.length; i += 1) {
      const result = event.results[i]
      const text = result[0]?.transcript || ''
      if (result.isFinal) {
        finalTranscript = `${finalTranscript} ${text}`.trim()
      } else {
        interimTranscript = `${interimTranscript} ${text}`.trim()
      }
    }
    updateTranscript(getDisplayText(interimTranscript))
  }

  rec.onerror = (event: any) => {
    console.error('语音识别错误:', event.error)
    if (event.error === 'not-allowed') {
      ElMessage.error('麦克风权限被拒绝，请在浏览器设置中开启权限')
      stopAudioMonitor()
      recording.value = false
      return
    }
    if (recording.value && !stoppingByUser) {
      useServerAsr.value = true
      stopAudioMonitor()
      void fallbackToServerAsr()
    } else if (event.error === 'no-speech') {
      updateTranscript(finalTranscript || '未识别到语音')
    }
  }

  rec.onend = () => {
    if (recording.value && !useServerAsr.value && !stoppingByUser && !restartingRecognition) {
      restartingRecognition = true
      window.setTimeout(() => {
        try {
          if (recording.value && !useServerAsr.value && !stoppingByUser) {
            rec.start()
          }
        } catch {
          /* browser may reject rapid restarts */
        } finally {
          restartingRecognition = false
        }
      }, 250)
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
    startAudioMonitor(stream)
    audioChunks = []
    mediaRecorder = new MediaRecorder(stream)
    mediaRecorder.ondataavailable = (e) => {
      if (e.data.size > 0) audioChunks.push(e.data)
    }
    mediaRecorder.onstop = async () => {
      stopAudioMonitor()
      if (!shouldUploadRecording) {
        audioChunks = []
        return
      }
      const blob = new Blob(audioChunks, { type: 'audio/webm' })
      recording.value = false
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
  transcribing.value = true
  try {
    const formData = new FormData()
    formData.append('audio', blob, 'recording.webm')
    formData.append('format', 'webm')
    if (props.sessionId) {
      formData.append('sessionId', String(props.sessionId))
    }
    const res = await interviewApi.convertAsr(formData)
    updateTranscript(res.text || '未识别到语音')
    emitTranscript(transcript.value)
    if (res.isMock) {
      ElMessage.info('当前使用 ASR 降级模式')
    }
  } catch {
    ElMessage.error('语音转写失败，请改用文字输入')
    updateTranscript('未识别到语音')
    emitTranscript(transcript.value)
  } finally {
    recording.value = false
    useServerAsr.value = false
    transcribing.value = false
  }
}

async function toggleRecording() {
  if (recording.value) {
    stopRecording()
    return
  }

  resetTranscript()
  stoppingByUser = false
  shouldUploadRecording = true
  useServerAsr.value = false

  if (SpeechRecognition) {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      startAudioMonitor(stream)
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

function stopRecording(emitEmptyResult = true) {
  stoppingByUser = true
  shouldUploadRecording = emitEmptyResult
  if (!recording.value && !transcribing.value) {
    return
  }
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
  stopAudioMonitor()
  recording.value = false
  useServerAsr.value = false
  if (emitEmptyResult) {
    updateTranscript(finalTranscript || transcript.value || '未识别到语音')
    emitTranscript(transcript.value)
  }
  shouldUploadRecording = true
}

onUnmounted(() => {
  stopRecording(false)
  recognition = null
  mediaRecorder = null
})
</script>

<style scoped>
.voice-wave {
  display: inline-flex;
  height: 32px;
  width: 92px;
  align-items: center;
  justify-content: center;
  gap: 4px;
  border-radius: 999px;
  background: #fef2f2;
  padding: 0 10px;
}

.voice-wave__bar {
  width: 4px;
  min-height: 6px;
  max-height: 28px;
  border-radius: 999px;
  background: #ef4444;
  transition: height 90ms ease-out;
}
</style>
