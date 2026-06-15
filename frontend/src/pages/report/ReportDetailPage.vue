<template>
  <div v-loading="loading" class="max-w-5xl mx-auto space-y-6">
    <div v-if="report" class="flex items-center justify-between flex-wrap gap-3">
      <div>
        <h1 class="text-2xl font-bold">{{ report.positionName }}</h1>
        <p class="text-slate-500">综合得分：{{ report.overallScore ?? '-' }}</p>
      </div>
      <div class="flex items-center gap-2">
        <el-tag :type="report.reportStatus === 'COMPLETED' ? 'success' : 'warning'">
          {{ report.reportStatus }}
        </el-tag>
        <el-button
          v-if="report.reportStatus === 'COMPLETED'"
          :loading="downloading"
          @click="handleDownload"
        >
          下载PDF
        </el-button>
        <el-button v-if="report.reportStatus === 'COMPLETED'" @click="handleShare">分享报告</el-button>
      </div>
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

    <el-card v-if="reportId && report?.reportStatus === 'COMPLETED'">
      <template #header>推荐学习资源</template>
      <div v-loading="recLoading">
        <el-empty v-if="!recommendations.length" description="暂无推荐" />
        <div v-for="item in recommendations" :key="item.recommendationId" class="border-b py-4 last:border-0">
          <h3 class="font-semibold">{{ item.resource.title }}</h3>
          <p class="text-sm text-slate-500 mt-1">{{ item.resource.description }}</p>
          <p v-if="item.reason" class="text-xs text-indigo-600 mt-1">推荐原因：{{ item.reason }}</p>
          <div class="mt-2 flex gap-2">
            <el-button v-if="item.resource.url" type="primary" link :href="item.resource.url" target="_blank">
              打开链接
            </el-button>
            <el-button size="small" @click="feedback(item.recommendationId, true)">有帮助</el-button>
            <el-button size="small" @click="feedback(item.recommendationId, false)">没帮助</el-button>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { marked } from 'marked'
import { ElMessage } from 'element-plus'
import * as reportApi from '@/api/report'
import * as resourceApi from '@/api/resource'
import RadarChart from '@/components/charts/RadarChart.vue'
import type { QuestionScore, ReportDetail } from '@/types'
import type { RecommendationItem } from '@/api/resource'

const route = useRoute()
const reportId = computed(() => Number(route.params.reportId))
const loading = ref(false)
const recLoading = ref(false)
const downloading = ref(false)
const report = ref<ReportDetail | null>(null)
const recommendations = ref<RecommendationItem[]>([])

const questionScores = computed(() => (report.value?.questionScores || []) as QuestionScore[])

const summaryHtml = computed(() =>
  report.value?.summary ? (marked.parse(report.value.summary, { async: false }) as string) : '',
)

async function loadReport() {
  loading.value = true
  try {
    report.value = await reportApi.getReport(reportId.value)
    if (report.value?.reportStatus === 'COMPLETED') {
      await loadRecommendations()
    }
  } finally {
    loading.value = false
  }
}

async function loadRecommendations() {
  recLoading.value = true
  try {
    const res = await resourceApi.getRecommendations(reportId.value)
    recommendations.value = res.recommendations
  } finally {
    recLoading.value = false
  }
}

async function feedback(recommendationId: number, isHelpful: boolean) {
  await resourceApi.feedbackRecommendation(recommendationId, isHelpful)
  ElMessage.success('感谢反馈')
}

async function handleShare() {
  const res = await reportApi.shareReport(reportId.value)
  const url = res.shareUrl || `${window.location.origin}/share/${res.shareToken}`
  try {
    await navigator.clipboard.writeText(url)
    ElMessage.success('分享链接已复制到剪贴板')
  } catch {
    ElMessage.success(`分享链接：${url}`)
  }
}

async function handleDownload() {
  if (!report.value) return
  downloading.value = true
  try {
    const blob = await reportApi.downloadReportPdf(reportId.value)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${safeFileName(report.value.positionName)}-面试报告.pdf`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
    ElMessage.success('报告下载已开始')
  } finally {
    downloading.value = false
  }
}

function safeFileName(value: string) {
  return (value || 'AI模拟面试')
    .replace(/[\\/:*?"<>|]/g, '-')
    .replace(/\s+/g, ' ')
    .trim()
}

onMounted(loadReport)
</script>
