<template>
  <div class="max-w-5xl mx-auto space-y-6">
    <div class="flex flex-wrap items-center justify-between gap-4">
      <h1 class="text-2xl font-bold">能力成长曲线</h1>
      <div class="flex gap-3">
        <el-select v-model="positionCode" placeholder="全部岗位" clearable style="width: 200px">
          <el-option v-for="p in positions" :key="p.code" :label="p.name" :value="p.code" />
        </el-select>
        <el-select v-model="days" style="width: 120px">
          <el-option :value="30" label="近 30 天" />
          <el-option :value="90" label="近 90 天" />
          <el-option :value="180" label="近 180 天" />
        </el-select>
        <el-button type="primary" @click="load">刷新</el-button>
      </div>
    </div>

    <el-row v-if="data?.trend" :gutter="16">
      <el-col :span="8">
        <el-statistic title="综合分变化" :value="data.trend.overallChange" suffix="分" />
      </el-col>
      <el-col :span="8">
        <el-statistic title="最强维度" :value="data.trend.strongestDimension" />
      </el-col>
      <el-col :span="8">
        <el-statistic title="最弱维度" :value="data.trend.weakestDimension" />
      </el-col>
    </el-row>

    <el-card v-loading="loading">
      <GrowthLineChart v-if="data?.records?.length" :records="data.records" />
      <el-empty v-else description="暂无成长数据，完成面试后将自动记录" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import * as growthApi from '@/api/growth'
import * as positionApi from '@/api/position'
import GrowthLineChart from '@/components/charts/GrowthLineChart.vue'
import type { GrowthData, Position } from '@/types'

const loading = ref(false)
const data = ref<GrowthData | null>(null)
const positions = ref<Position[]>([])
const positionCode = ref('')
const days = ref(90)

async function load() {
  loading.value = true
  try {
    data.value = await growthApi.getGrowth({
      positionCode: positionCode.value || undefined,
      days: days.value,
    })
  } finally {
    loading.value = false
  }
}

watch([positionCode, days], load)

onMounted(async () => {
  positions.value = await positionApi.listPositions()
  await load()
})
</script>
