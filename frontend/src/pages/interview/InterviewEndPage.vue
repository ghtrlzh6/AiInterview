<template>
  <div class="max-w-lg mx-auto text-center py-16">
    <el-icon :size="64" class="text-indigo-500 mb-4"><CircleCheckFilled /></el-icon>
    <h1 class="text-2xl font-bold text-slate-800 mb-2">面试已结束</h1>
    <p class="text-slate-500 mb-8">正在生成多维度评估报告，请稍候...</p>

    <el-progress
      v-if="polling"
      :percentage="progress"
      :indeterminate="report?.reportStatus === 'GENERATING'"
      class="mb-8"
    />

    <div class="flex flex-col gap-3">
      <el-button
        v-if="reportId"
        type="primary"
        size="large"
        :disabled="report?.reportStatus !== 'COMPLETED'"
        @click="router.push(`/reports/${reportId}`)"
      >
        {{ report?.reportStatus === 'COMPLETED' ? '查看完整报告' : '报告生成中...' }}
      </el-button>
      <el-button @click="router.push('/')">返回首页</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as reportApi from '@/api/report'
import type { ReportDetail } from '@/types'

const route = useRoute()
const router = useRouter()
const reportId = ref(Number(route.query.reportId) || 0)
const report = ref<ReportDetail | null>(null)
const polling = ref(false)
const progress = ref(30)
let timer: ReturnType<typeof setInterval> | null = null

async function pollReport() {
  if (!reportId.value) return
  polling.value = true
  try {
    report.value = await reportApi.getReport(reportId.value)
    if (report.value.reportStatus === 'COMPLETED') {
      progress.value = 100
      if (timer) clearInterval(timer)
    } else {
      progress.value = Math.min(progress.value + 10, 90)
    }
  } catch {
    /* retry */
  }
}

onMounted(() => {
  pollReport()
  timer = setInterval(pollReport, 3000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>
