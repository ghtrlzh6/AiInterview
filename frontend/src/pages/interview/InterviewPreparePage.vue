<template>
  <div class="mx-auto max-w-5xl">
    <div class="mb-6">
      <div>
        <h1 class="text-2xl font-bold text-slate-800">面试准备</h1>
        <p class="mt-2 text-slate-500">可以先检查设备、了解题型与授权说明，也可以跳过准备直接开始。</p>
      </div>
    </div>

    <section class="grid gap-4 lg:grid-cols-[1.15fr_0.85fr]">
      <div class="space-y-4">
        <div class="rounded-lg border bg-white p-5">
          <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 class="text-lg font-semibold text-slate-800">{{ interview.positionName || '模拟面试' }}</h2>
              <p class="mt-1 text-sm text-slate-500">共 {{ interview.totalQuestions || '-' }} 题，回答后 AI 面试官会继续追问。</p>
            </div>
            <el-tag effect="plain">{{ inputModeLabel }}</el-tag>
          </div>

          <div class="grid gap-3 md:grid-cols-2">
            <div class="prep-item">
              <el-icon class="prep-item__icon"><Timer /></el-icon>
              <div>
                <div class="font-medium text-slate-800">答题节奏</div>
                <div class="text-sm text-slate-500">每题建议 2-4 分钟</div>
              </div>
            </div>
            <div class="prep-item">
              <el-icon class="prep-item__icon"><DocumentChecked /></el-icon>
              <div>
                <div class="font-medium text-slate-800">代码题操作</div>
                <div class="text-sm text-slate-500">先提交代码，再说明思路</div>
              </div>
            </div>
          </div>
        </div>

        <div class="coding-panel rounded-lg border bg-white p-5">
          <div class="mb-3 flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 class="text-lg font-semibold text-slate-800">手撕代码样例题</h2>
              <p class="text-sm text-slate-500">这与正式面试中的代码题区域一致，用来熟悉操作。</p>
            </div>
            <el-select v-model="sampleLanguage" class="w-36">
              <el-option label="Java" value="java" />
              <el-option label="TypeScript" value="typescript" />
              <el-option label="Python" value="python" />
              <el-option label="C++" value="cpp" />
              <el-option label="C#" value="csharp" />
            </el-select>
          </div>

          <div class="mb-3 rounded border border-slate-200 bg-slate-50 p-3 text-sm text-slate-700">
            <div class="flex flex-wrap items-center gap-2">
              <el-tag type="danger" effect="plain">示例</el-tag>
              <span class="font-medium text-slate-800">{{ codingSample.title }}</span>
            </div>
            <p class="mt-3 leading-6">{{ codingSample.description }}</p>
            <div class="mt-3 flex flex-wrap gap-2">
              <el-tag v-for="tag in codingSample.tags" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
            </div>
          </div>

          <el-input
            v-model="sampleCode"
            type="textarea"
            :rows="10"
            resize="vertical"
            placeholder="在这里输入代码..."
            class="code-input"
          />

          <div class="mt-3 flex flex-wrap items-center justify-between gap-3">
            <div class="text-sm text-slate-500">提交后，正式面试中请在左侧回答区补充思路与复杂度。</div>
            <el-button type="primary" :icon="CircleCheck" :disabled="!sampleCode.trim()" @click="submitSampleCode">
              提交代码
            </el-button>
          </div>
        </div>
      </div>

      <div class="rounded-lg border bg-white p-5">
        <h2 class="text-lg font-semibold text-slate-800">开始前检查</h2>

        <div v-if="interview.inputMode === 'VOICE'" class="mt-4 rounded-lg border border-slate-200 p-4">
          <div class="flex items-start justify-between gap-3">
            <div>
              <div class="font-medium text-slate-800">麦克风检测</div>
              <div class="mt-1 text-sm text-slate-500">{{ microphoneHint }}</div>
            </div>
            <el-tag :type="microphoneTagType" effect="plain">{{ microphoneStatusLabel }}</el-tag>
          </div>
          <el-button
            class="mt-4"
            type="primary"
            plain
            :loading="microphoneChecking"
            @click="checkMicrophone"
          >
            <el-icon><Microphone /></el-icon>
            <span>检测麦克风</span>
          </el-button>
        </div>

        <div v-else class="mt-4 rounded-lg border border-slate-200 bg-slate-50 p-4 text-sm text-slate-500">
          当前为文字输入模式，无需麦克风检测。
        </div>

        <button
          type="button"
          class="privacy-card mt-4"
          :class="{ 'privacy-card--checked': privacyAgreed }"
          @click="privacyAgreed = !privacyAgreed"
        >
          <el-checkbox
            :model-value="privacyAgreed"
            class="privacy-checkbox"
            @click.stop
            @change="privacyAgreed = Boolean($event)"
          />
          <span class="text-left text-sm leading-6 text-slate-600">
            我已了解本次面试会记录输入内容、语音转写文本和作答过程，用于生成模拟面试反馈。
          </span>
        </button>

        <div class="mt-6 grid gap-3 sm:grid-cols-2">
          <el-button size="large" plain @click="enterInterview">
            跳过准备
          </el-button>
          <el-button size="large" type="primary" @click="startAfterPrepare">
            开始正式面试
          </el-button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CircleCheck } from '@element-plus/icons-vue'
import { useInterviewStore } from '@/stores/interview'

type MicrophoneStatus = 'idle' | 'success' | 'failed' | 'unsupported'

const route = useRoute()
const router = useRouter()
const interview = useInterviewStore()

const privacyAgreed = ref(false)
const microphoneChecking = ref(false)
const microphoneStatus = ref<MicrophoneStatus>('idle')
const microphoneDevice = ref('')
const sampleLanguage = ref('java')
const sampleCode = ref('')

const sessionId = computed(() => Number(route.params.sessionId))

const inputModeLabel = computed(() => (interview.inputMode === 'VOICE' ? '语音作答' : '文字作答'))

const codingSample = {
  title: 'A + B',
  description: '输入两个整数 A 和 B，输出它们的和。示例输入：1 2，示例输出：3。',
  tags: ['输入输出', '基础语法', '热身题'],
}

const sampleCodeMap: Record<string, string> = {
  java: `import java.util.Scanner;

public class Main {
  public static void main(String[] args) {
    Scanner sc = new Scanner(System.in);
    int a = sc.nextInt();
    int b = sc.nextInt();
    System.out.println(a + b);
  }
}`,
  typescript: `const fs = require('fs');
const [a, b] = fs.readFileSync(0, 'utf8').trim().split(/\\s+/).map(Number);
console.log(a + b);`,
  python: `a, b = map(int, input().split())
print(a + b)`,
  cpp: `#include <iostream>
using namespace std;

int main() {
  int a, b;
  cin >> a >> b;
  cout << a + b << endl;
  return 0;
}`,
  csharp: `using System;

class Program {
  static void Main() {
    var parts = Console.ReadLine()!.Split(' ');
    Console.WriteLine(int.Parse(parts[0]) + int.Parse(parts[1]));
  }
}`,
}

const microphoneStatusLabel = computed(() => {
  if (microphoneStatus.value === 'success') return '已通过'
  if (microphoneStatus.value === 'failed') return '未通过'
  if (microphoneStatus.value === 'unsupported') return '不支持'
  return '待检测'
})

const microphoneTagType = computed(() => {
  if (microphoneStatus.value === 'success') return 'success'
  if (microphoneStatus.value === 'failed' || microphoneStatus.value === 'unsupported') return 'danger'
  return 'info'
})

const microphoneHint = computed(() => {
  if (microphoneStatus.value === 'success') {
    return microphoneDevice.value ? `已识别：${microphoneDevice.value}` : '浏览器已允许访问麦克风'
  }
  if (microphoneStatus.value === 'failed') return '请允许浏览器麦克风权限后重新检测'
  if (microphoneStatus.value === 'unsupported') return '当前浏览器不支持麦克风访问'
  return '语音模式建议先完成麦克风权限检测'
})

function enterInterview() {
  router.push({ name: 'interview-room', params: { sessionId: String(sessionId.value) } })
}

function startAfterPrepare() {
  if (!privacyAgreed.value) {
    ElMessage.warning('请先确认隐私授权，或选择跳过准备')
    return
  }
  if (interview.inputMode === 'VOICE' && microphoneStatus.value !== 'success') {
    ElMessage.warning('请先完成麦克风检测，或选择跳过准备')
    return
  }
  enterInterview()
}

function submitSampleCode() {
  ElMessage.success('示例代码已提交。正式面试中提交后，请继续说明你的解题思路。')
}

async function checkMicrophone() {
  if (!navigator.mediaDevices?.getUserMedia) {
    microphoneStatus.value = 'unsupported'
    return
  }

  microphoneChecking.value = true
  microphoneStatus.value = 'idle'
  microphoneDevice.value = ''
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    const track = stream.getAudioTracks()[0]
    microphoneDevice.value = track?.label || ''
    stream.getTracks().forEach((item) => item.stop())
    microphoneStatus.value = 'success'
    ElMessage.success('麦克风检测通过')
  } catch {
    microphoneStatus.value = 'failed'
    ElMessage.error('麦克风检测失败，请检查浏览器权限')
  } finally {
    microphoneChecking.value = false
  }
}

onMounted(async () => {
  try {
    if (interview.sessionId !== sessionId.value || !interview.messages.length) {
      const detail = await interview.restore(sessionId.value)
      if (detail.sessionStatus !== 'IN_PROGRESS') {
        router.replace({ name: 'interview-end', params: { sessionId: String(sessionId.value) } })
      }
    }
  } catch {
    ElMessage.error('面试准备页加载失败，请重新选择岗位')
    router.replace('/interview/select')
  }
})

watch(
  sampleLanguage,
  (language) => {
    sampleCode.value = sampleCodeMap[language] || ''
  },
  { immediate: true },
)
</script>

<style scoped>
.prep-item {
  display: flex;
  min-height: 92px;
  gap: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 14px;
  background: #ffffff;
}

.prep-item__icon {
  flex: 0 0 auto;
  margin-top: 2px;
  color: #4f46e5;
  font-size: 20px;
}

.privacy-checkbox {
  flex: 0 0 auto;
  height: 22px;
}

.privacy-card {
  display: flex;
  width: 100%;
  align-items: flex-start;
  gap: 10px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
  padding: 12px;
  text-align: left;
  transition: border-color 0.15s ease, background-color 0.15s ease;
}

.privacy-card:hover,
.privacy-card--checked {
  border-color: #818cf8;
  background: #eef2ff;
}

.code-input :deep(textarea) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', monospace;
}
</style>
