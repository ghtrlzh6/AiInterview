<template>
  <div class="max-w-5xl mx-auto space-y-8">
    <section class="rounded-2xl bg-gradient-to-r from-indigo-600 to-violet-600 text-white p-8 shadow-lg">
      <h1 class="text-3xl font-bold mb-2">
        你好，{{ auth.userInfo?.nickname || '同学' }} 👋
      </h1>
      <p class="text-indigo-100 mb-6">用 AI 模拟真实面试，多维度反馈助你快速成长</p>
      <el-button size="large" type="warning" @click="router.push('/interview/select')">
        开始模拟面试
      </el-button>
    </section>

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
        <el-table-column prop="sessionStatus" label="状态" width="100" />
        <el-table-column prop="overallScore" label="得分" width="80">
          <template #default="{ row }">{{ row.overallScore ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button
              v-if="row.reportId"
              type="primary"
              link
              @click="router.push(`/reports/${row.reportId}`)"
            >
              查看报告
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import * as userApi from '@/api/user'
import type { InterviewHistoryItem } from '@/api/user'

const auth = useAuthStore()
const router = useRouter()
const loading = ref(false)
const history = ref<InterviewHistoryItem[]>([])

onMounted(async () => {
  loading.value = true
  try {
    const res = await userApi.getMyInterviews({ page: 1, size: 5 })
    history.value = res.list
  } catch {
    history.value = []
  } finally {
    loading.value = false
  }
})
</script>
