<template>
  <div v-loading="loading" class="min-h-screen bg-slate-50 py-8 px-4">
    <div class="max-w-5xl mx-auto space-y-6">
      <div class="text-center text-sm text-slate-500">AI 模拟面试 · 报告分享</div>

      <div v-if="report" class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-bold">{{ report.positionName }}</h1>
          <p class="text-slate-500">综合得分：{{ report.overallScore ?? '-' }}</p>
        </div>
        <el-tag type="success">分享报告</el-tag>
      </div>

      <el-row v-if="report?.scores" :gutter="16">
        <el-col :xs="24" :md="12">
          <el-card>
            <template #header>能力雷达图</template>
            <RadarChart :scores="report.scores" />
          </el-card>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-card>
            <template #header>亮点</template>
            <ul class="list-disc pl-5 space-y-2 text-sm">
              <li v-for="(h, i) in report.highlights || []" :key="i">{{ h }}</li>
            </ul>
          </el-card>
          <el-card class="mt-4">
            <template #header>待改进</template>
            <ul class="list-disc pl-5 space-y-2 text-sm text-amber-800">
              <li v-for="(w, i) in report.weaknesses || []" :key="i">{{ w }}</li>
            </ul>
          </el-card>
        </el-col>
      </el-row>

      <el-card v-if="report?.summary">
        <template #header>综合评估</template>
        <div class="markdown-body" v-html="summaryHtml" />
      </el-card>

      <el-card v-if="questionScores.length">
        <template #header>逐题点评</template>
        <div v-for="qs in questionScores" :key="qs.questionOrder" class="border-b py-4 last:border-0">
          <div class="flex items-center gap-2 mb-2">
            <el-tag size="small">第 {{ qs.questionOrder }} 题</el-tag>
            <span class="font-medium text-sm">{{ qs.questionTitle }}</span>
          </div>
          <div class="flex flex-wrap gap-3 text-xs text-slate-600 mb-2">
            <span>技术 {{ qs.techScore ?? '-' }}</span>
            <span>逻辑 {{ qs.logicScore ?? '-' }}</span>
            <span>深度 {{ qs.depthScore ?? '-' }}</span>
          </div>
          <p class="text-sm text-slate-700">{{ qs.comment }}</p>
        </div>
      </el-card>

      <el-card v-if="report?.suggestions?.length">
        <template #header>改进建议</template>
        <ul class="list-disc pl-5 space-y-2">
          <li v-for="(s, i) in report.suggestions" :key="i">{{ s }}</li>
        </ul>
      </el-card>

      <div class="text-center">
        <el-button type="primary" @click="router.push('/auth/login')">登录体验完整功能</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { marked } from 'marked'
import * as reportApi from '@/api/report'
import RadarChart from '@/components/charts/RadarChart.vue'
import type { QuestionScore, ReportDetail } from '@/types'

const route = useRoute()
const router = useRouter()
const shareToken = computed(() => String(route.params.token))
const loading = ref(false)
const report = ref<ReportDetail | null>(null)

const questionScores = computed(() => (report.value?.questionScores || []) as QuestionScore[])

const summaryHtml = computed(() =>
  report.value?.summary ? (marked.parse(report.value.summary, { async: false }) as string) : '',
)

onMounted(async () => {
  loading.value = true
  try {
    report.value = await reportApi.getSharedReport(shareToken.value)
  } finally {
    loading.value = false
  }
})
</script>
