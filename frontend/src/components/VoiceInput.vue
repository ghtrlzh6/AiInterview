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

defineProps<{
  sessionId?: number
  disabled?: boolean
}>()

const emit = defineEmits<{ transcribed: [text: string] }>()

const recording = ref(false)
const transcript = ref('')

// 获取浏览器语音识别对象
const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition
let recognition: any = null

function initRecognition() {
  if (!SpeechRecognition) {
    ElMessage.error('您的浏览器不支持语音识别功能，请使用Chrome浏览器')
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
    } else if (event.error === 'no-speech') {
      transcript.value = '未识别到语音'
      emit('transcribed', '未识别到语音')
    } else {
      ElMessage.error('语音识别失败: ' + event.error)
    }
    recording.value = false
  }
  
  rec.onend = () => {
    if (recording.value) {
      // 自动重新启动识别（用于连续识别场景）
      rec.start()
    }
  }
  
  return rec
}

async function toggleRecording() {
  if (recording.value) {
    stopRecording()
    return
  }
  
  try {
    // 先请求麦克风权限
    await navigator.mediaDevices.getUserMedia({ audio: true })
    
    if (!recognition) {
      recognition = initRecognition()
      if (!recognition) return
    }
    
    transcript.value = ''
    recording.value = true
    recognition.start()
  } catch (error) {
    ElMessage.error('无法访问麦克风，请检查权限设置')
    recording.value = false
  }
}

function stopRecording() {
  if (recognition) {
    recognition.stop()
  }
  recording.value = false
}

onUnmounted(() => {
  stopRecording()
  if (recognition) {
    recognition = null
  }
})
</script>
