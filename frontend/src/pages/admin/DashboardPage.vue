<template>
  <el-row :gutter="16" v-loading="loading">
    <el-col v-for="item in cards" :key="item.key" :xs="24" :sm="12" :md="8" :lg="4">
      <el-card shadow="hover" class="text-center">
        <div class="text-3xl font-bold text-indigo-600">{{ item.value }}</div>
        <div class="text-sm text-slate-500 mt-2">{{ item.label }}</div>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import * as adminApi from '@/api/admin'
import type { AdminStats } from '@/types'

const loading = ref(false)
const stats = ref<AdminStats | null>(null)

const cards = computed(() => {
  const s = stats.value
  return [
    { key: 'users', label: '总用户', value: s?.totalUsers ?? 0 },
    { key: 'interviews', label: '总面试', value: s?.totalInterviews ?? 0 },
    { key: 'reports', label: '总报告', value: s?.totalReports ?? 0 },
    { key: 'today', label: '今日面试', value: s?.todayInterviews ?? 0 },
    { key: 'active', label: '7日活跃', value: s?.activeUsers7d ?? 0 },
  ]
})

onMounted(async () => {
  loading.value = true
  try {
    stats.value = await adminApi.getAdminStats()
  } finally {
    loading.value = false
  }
})
</script>
