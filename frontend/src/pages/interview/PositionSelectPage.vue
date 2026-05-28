<template>
  <div class="max-w-4xl mx-auto">
    <h1 class="text-2xl font-bold text-slate-800 mb-2">选择面试岗位</h1>
    <p class="text-slate-500 mb-8">选择目标岗位，AI 面试官将围绕该岗位技术栈提问</p>

    <el-form label-position="top" class="mb-6 bg-white p-6 rounded-xl border">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="输入方式">
            <el-radio-group v-model="interview.inputMode">
              <el-radio value="TEXT">文字</el-radio>
              <el-radio value="VOICE">语音</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="题目数量">
            <el-slider v-model="questionCount" :min="3" :max="15" show-input />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <div v-loading="loading" class="grid gap-4 md:grid-cols-2">
      <el-card
        v-for="pos in positions"
        :key="pos.code"
        shadow="hover"
        class="cursor-pointer transition hover:ring-2 hover:ring-indigo-400"
        :class="{ 'ring-2 ring-indigo-500': selected === pos.code }"
        @click="selected = pos.code"
      >
        <div class="flex justify-between items-start">
          <div>
            <h3 class="font-semibold text-lg">{{ pos.name }}</h3>
            <p class="text-sm text-slate-500 mt-2 line-clamp-2">{{ pos.description }}</p>
            <div class="flex flex-wrap gap-1 mt-3">
              <el-tag v-for="t in (pos.techStack || []).slice(0, 4)" :key="t" size="small">{{ t }}</el-tag>
            </div>
          </div>
          <el-icon v-if="selected === pos.code" class="text-indigo-500" :size="24">
            <CircleCheckFilled />
          </el-icon>
        </div>
      </el-card>
    </div>

    <div class="mt-8 flex justify-center">
      <el-button
        type="primary"
        size="large"
        :disabled="!selected"
        :loading="starting"
        @click="startInterview"
      >
        进入面试间
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as positionApi from '@/api/position'
import { useInterviewStore } from '@/stores/interview'
import type { Position } from '@/types'

const router = useRouter()
const interview = useInterviewStore()
const loading = ref(true)
const starting = ref(false)
const positions = ref<Position[]>([])
const selected = ref('')
const questionCount = ref(8)

onMounted(async () => {
  try {
    positions.value = await positionApi.listPositions()
  } finally {
    loading.value = false
  }
})

async function startInterview() {
  if (!selected.value) return
  starting.value = true
  interview.reset()
  try {
    const res = await interview.start(selected.value, { questionCount: questionCount.value })
    router.push({ name: 'interview-room', params: { sessionId: String(res.sessionId) } })
  } catch {
    ElMessage.error('创建面试失败')
  } finally {
    starting.value = false
  }
}
</script>
