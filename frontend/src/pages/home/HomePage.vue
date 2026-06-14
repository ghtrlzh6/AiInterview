<template>
  <div class="max-w-5xl mx-auto space-y-8">
    <section class="rounded-2xl bg-gradient-to-r from-indigo-600 to-violet-600 text-white p-8 shadow-lg">
      <h1 class="text-3xl font-bold mb-2">
        你好，{{ auth.userInfo?.nickname || '同学' }}
      </h1>
      <p class="text-indigo-100 mb-6">用 AI 模拟真实面试，多维度反馈助你快速成长</p>
      <el-button size="large" type="warning" @click="router.push('/interview/select')">
        开始模拟面试
      </el-button>
    </section>

    <el-row :gutter="16">
      <el-col :xs="24" :sm="12">
        <el-card shadow="hover" v-loading="statsLoading">
          <el-statistic title="累计面试次数" :value="stats.interviewCount">
            <template #suffix>次</template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-card shadow="hover" v-loading="statsLoading">
          <el-statistic
            title="最近面试得分"
            :value="stats.latestScore ?? '-'"
            :value-style="scoreStyle"
          >
            <template v-if="stats.latestScore != null" #suffix>分</template>
          </el-statistic>
          <p v-if="stats.latestPositionName" class="text-xs text-slate-500 mt-2">
            {{ stats.latestPositionName }} · {{ stats.latestDate }}
          </p>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover" class="cursor-pointer" @click="router.push('/reports')">
          <div class="flex items-center gap-4">
            <el-icon :size="40" class="text-indigo-500"><Document /></el-icon>
            <div>
              <div class="font-semibold">评估报告</div>
              <div class="text-sm text-slate-500">查看历史面试分析</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover" class="cursor-pointer" @click="router.push('/growth')">
          <div class="flex items-center gap-4">
            <el-icon :size="40" class="text-emerald-500"><TrendCharts /></el-icon>
            <div>
              <div class="font-semibold">成长曲线</div>
              <div class="text-sm text-slate-500">能力变化一目了然</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover" class="cursor-pointer" @click="router.push('/knowledge')">
          <div class="flex items-center gap-4">
            <el-icon :size="40" class="text-amber-500"><Reading /></el-icon>
            <div>
              <div class="font-semibold">知识库</div>
              <div class="text-sm text-slate-500">系统复习面试知识点</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card v-loading="loading">
      <template #header>
        <span class="font-semibold">最近面试</span>
      </template>
      <el-empty v-if="!history.length" description="暂无面试记录，快去开始第一场吧" />
      <el-table v-else :data="history" stripe>
        <el-table-column prop="positionName" label="岗位" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">{{ formatStatus(row.sessionStatus) }}</template>
        </el-table-column>
        <el-table-column prop="overallScore" label="得分" width="80">
          <template #default="{ row }">{{ row.overallScore ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button
              v-if="row.sessionStatus === 'IN_PROGRESS'"
              type="primary"
              link
              @click="router.push(`/interview/${row.sessionId}`)"
            >
              继续面试
            </el-button>
            <el-button
              v-else-if="row.reportId"
              type="primary"
              link
              @click="router.push(`/reports/${row.reportId}`)"
            >
              查看报告
            </el-button>
            <el-button
              v-else-if="row.canGenerateReport"
              type="primary"
              link
              :loading="generatingSessionId === row.sessionId"
              @click="generateReport(row.sessionId)"
            >
              生成报告
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="mt-4 flex justify-center">
        <el-button type="primary" plain @click="router.push('/interviews')">
          所有面试
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useInterviewStore } from '@/stores/interview'
import * as userApi from '@/api/user'
import * as reportApi from '@/api/report'
import * as growthApi from '@/api/growth'
import type { InterviewHistoryItem } from '@/api/user'

const auth = useAuthStore()
const router = useRouter()
const interview = useInterviewStore()
const loading = ref(false)
const statsLoading = ref(false)
const generatingSessionId = ref<number | null>(null)
const history = ref<InterviewHistoryItem[]>([])

const stats = ref({
  interviewCount: 0,
  latestScore: null as number | null,
  latestPositionName: '',
  latestDate: '',
})

const scoreStyle = computed(() =>
  stats.value.latestScore != null ? { color: '#4f46e5', fontWeight: 'bold' } : {},
)

const STATUS_LABELS: Record<string, string> = {
  IN_PROGRESS: '进行中',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}

function formatStatus(status: string) {
  return STATUS_LABELS[status] ?? status
}

function formatDate(value?: string) {
  if (!value) return ''
  return value.slice(0, 10)
}

async function loadStats() {
  statsLoading.value = true
  try {
    const [reports, growth] = await Promise.all([
      reportApi.listReports({ page: 1, size: 1 }),
      growthApi.getGrowth({ days: 365 }),
    ])

    stats.value.interviewCount = reports.total

    const latestReport = reports.list[0]
    if (latestReport?.overallScore != null) {
      stats.value.latestScore = latestReport.overallScore
      stats.value.latestPositionName = latestReport.positionName ?? ''
      stats.value.latestDate = formatDate(latestReport.createdAt)
      return
    }

    const records = growth.records ?? []
    const latestGrowth = records[records.length - 1]
    if (latestGrowth) {
      stats.value.latestScore = latestGrowth.overallScore
      stats.value.latestDate = formatDate(latestGrowth.recordDate)
    }
  } catch {
    stats.value = {
      interviewCount: 0,
      latestScore: null,
      latestPositionName: '',
      latestDate: '',
    }
  } finally {
    statsLoading.value = false
  }
}

async function loadHistory() {
  loading.value = true
  try {
    const res = await userApi.getMyInterviews({ page: 1, size: 5 })
    history.value = res.list
  } catch {
    history.value = []
  } finally {
    loading.value = false
  }
}

async function generateReport(sessionId: number) {
  generatingSessionId.value = sessionId
  try {
    const res = await interview.generateReportForSession(sessionId)
    if (res.reportId) {
      ElMessage.success('已开始生成报告')
      router.push({
        name: 'interview-end',
        params: { sessionId: String(sessionId) },
        query: { reportId: String(res.reportId) },
      })
    } else {
      await loadHistory()
    }
  } finally {
    generatingSessionId.value = null
  }
}

onMounted(async () => {
  await Promise.all([auth.fetchProfile(), loadStats(), loadHistory()])
})
</script>
