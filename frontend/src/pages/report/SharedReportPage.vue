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
        <template #header>
          <div class="flex items-center justify-between">
            <span>逐题点评</span>
            <span class="text-xs text-slate-500">
              AI评分 + 回答回顾
            </span>
          </div>
        </template>

        <div
          v-for="qs in questionScores"
          :key="qs.questionOrder"
          class="border-b py-6 last:border-0"
        >
          <!-- 题目 -->
          <div class="flex items-center gap-2 mb-3">
            <el-tag size="small">
              第 {{ qs.questionOrder }} 题
            </el-tag>

            <span class="font-medium">
              {{ qs.questionTitle }}
            </span>
          </div>

          <!-- 分数 -->
          <div class="flex flex-wrap gap-3 mb-4">
            <el-tag type="primary">
              技术 {{ qs.techScore ?? '-' }}
            </el-tag>

            <el-tag type="success">
              逻辑 {{ qs.logicScore ?? '-' }}
            </el-tag>

            <el-tag type="warning">
              深度 {{ qs.depthScore ?? '-' }}
            </el-tag>
          </div>

          <!-- 用户回答 -->
          <div class="mb-4">
            <div class="text-xs text-slate-500 mb-2">
              我的回答
            </div>

            <div
              class="bg-slate-50 rounded-lg p-3 text-sm whitespace-pre-wrap border"
            >
              {{ qs.userAnswer || '暂无回答记录' }}
            </div>
          </div>

          <!-- 参考答案 -->
          <div class="mb-4">
            <div class="text-xs text-green-700 mb-2">
              参考答案
            </div>

            <div
              class="bg-green-50 rounded-lg p-3 text-sm whitespace-pre-wrap border border-green-200"
            >
              {{ qs.referenceAnswer || '暂无参考答案' }}
            </div>
          </div>

          <!-- AI点评 -->
          <div>
            <div class="text-xs text-blue-700 mb-2">
              AI点评
            </div>

            <div
              class="bg-blue-50 rounded-lg p-3 text-sm whitespace-pre-wrap border border-blue-200"
            >
              {{ qs.comment || '暂无点评' }}
            </div>
          </div>
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
