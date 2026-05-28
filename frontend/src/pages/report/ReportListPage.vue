<template>
  <div class="max-w-5xl mx-auto">
    <h1 class="text-2xl font-bold mb-6">评估报告</h1>
    <el-card v-loading="loading">
      <el-table :data="list" stripe>
        <el-table-column prop="positionName" label="岗位" />
        <el-table-column prop="overallScore" label="综合得分" width="100">
          <template #default="{ row }">{{ row.overallScore ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="reportStatus" label="状态" width="120" />
        <el-table-column prop="createdAt" label="时间" width="180" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="primary" link @click="router.push(`/reports/${row.reportId}`)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
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
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import * as reportApi from '@/api/report'
import type { ReportListItem } from '@/api/report'

const router = useRouter()
const loading = ref(false)
const list = ref<ReportListItem[]>([])
const page = ref(1)
const size = ref(10)
const total = ref(0)

async function load() {
  loading.value = true
  try {
    const res = await reportApi.listReports({ page: page.value, size: size.value })
    list.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
