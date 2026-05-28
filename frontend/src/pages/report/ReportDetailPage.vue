<template>
  <div v-loading="loading" class="max-w-5xl mx-auto space-y-6">
    <div v-if="report" class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold">{{ report.positionName }}</h1>
        <p class="text-slate-500">综合得分：{{ report.overallScore ?? '-' }}</p>
      </div>
      <el-tag :type="report.reportStatus === 'COMPLETED' ? 'success' : 'warning'">
        {{ report.reportStatus }}
      </el-tag>
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

    <el-card v-if="report?.suggestions?.length">
      <template #header>改进建议</template>
      <ul class="list-disc pl-5 space-y-2">
        <li v-for="(s, i) in report.suggestions" :key="i">{{ s }}</li>
      </ul>
    </el-card>

    <el-button v-if="reportId" type="primary" @click="router.push({ path: '/resources', query: { reportId } })">
      查看推荐学习资源
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { marked } from 'marked'
import * as reportApi from '@/api/report'
import RadarChart from '@/components/charts/RadarChart.vue'
import type { ReportDetail } from '@/types'

const route = useRoute()
const router = useRouter()
const reportId = computed(() => Number(route.params.reportId))
const loading = ref(false)
const report = ref<ReportDetail | null>(null)

const summaryHtml = computed(() =>
  report.value?.summary ? (marked.parse(report.value.summary, { async: false }) as string) : '',
)

onMounted(async () => {
  loading.value = true
  try {
    report.value = await reportApi.getReport(reportId.value)
  } finally {
    loading.value = false
  }
})
</script>
