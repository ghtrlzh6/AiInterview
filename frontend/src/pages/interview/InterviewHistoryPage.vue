<template>
  <div class="max-w-6xl mx-auto space-y-6">
    <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h1 class="text-2xl font-bold text-slate-800">所有面试</h1>
        <p class="text-sm text-slate-500 mt-1">查看全部面试记录，并为已完成的面试生成报告。</p>
      </div>
      <el-button type="primary" @click="router.push('/interview/select')">
        开始面试
      </el-button>
    </div>

    <el-card v-loading="loading">
      <el-empty v-if="!list.length" description="暂无面试记录" />
      <el-table v-else :data="list" stripe>
        <el-table-column prop="positionName" label="岗位" min-width="140" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.sessionStatus)">
              {{ formatStatus(row.sessionStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="报告" width="130">
          <template #default="{ row }">
            <span v-if="row.reportId">
              {{ row.overallScore ?? '-' }} 分
            </span>
            <el-tag v-else-if="row.canGenerateReport" type="warning">未生成</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="时长" width="110">
          <template #default="{ row }">{{ formatDuration(row.durationSeconds) }}</template>
        </el-table-column>
        <el-table-column label="开始时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="结束时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.endTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
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
              v-if="row.reportId"
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

      <el-pagination
        v-if="total > size"
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="prev, pager, next"
        class="mt-4 justify-end"
        @current-change="load"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as userApi from '@/api/user'
import { useInterviewStore } from '@/stores/interview'
import type { InterviewHistoryItem } from '@/api/user'

const router = useRouter()
const interview = useInterviewStore()
const loading = ref(false)
const list = ref<InterviewHistoryItem[]>([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const generatingSessionId = ref<number | null>(null)

const STATUS_LABELS: Record<string, string> = {
  IN_PROGRESS: '进行中',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}

function formatStatus(status: string) {
  return STATUS_LABELS[status] ?? status
}

function statusTagType(status: string) {
  if (status === 'IN_PROGRESS') return 'primary'
  if (status === 'COMPLETED') return 'success'
  if (status === 'CANCELLED') return 'info'
  return 'info'
}

function formatDateTime(value?: string) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

function formatDuration(seconds?: number) {
  if (!seconds) return '-'
  const minutes = Math.floor(seconds / 60)
  const rest = seconds % 60
  if (minutes <= 0) return `${rest} 秒`
  return `${minutes} 分 ${rest} 秒`
}

async function load() {
  loading.value = true
  try {
    const res = await userApi.getMyInterviews({ page: page.value, size: size.value })
    list.value = res.list
    total.value = res.total
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
      return
    }
    await load()
  } finally {
    generatingSessionId.value = null
  }
}

onMounted(load)
</script>
