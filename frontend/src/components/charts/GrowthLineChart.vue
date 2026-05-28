<template>
  <div ref="chartRef" class="w-full" :style="{ height: height }" />
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'
import type { GrowthRecord } from '@/types'

const props = withDefaults(
  defineProps<{
    records: GrowthRecord[]
    height?: string
  }>(),
  { height: '360px' },
)

const chartRef = ref<HTMLElement | null>(null)
let chart: echarts.ECharts | null = null

function render() {
  if (!chartRef.value || !props.records.length) return
  if (!chart) chart = echarts.init(chartRef.value)

  const dates = props.records.map((r) => r.recordDate)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['综合', '技术', '表达', '逻辑', '深度', '自信'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value', min: 0, max: 100 },
    series: [
      { name: '综合', type: 'line', smooth: true, data: props.records.map((r) => r.overallScore) },
      { name: '技术', type: 'line', smooth: true, data: props.records.map((r) => r.techScore) },
      {
        name: '表达',
        type: 'line',
        smooth: true,
        data: props.records.map((r) => r.expressionScore),
      },
      { name: '逻辑', type: 'line', smooth: true, data: props.records.map((r) => r.logicScore) },
      { name: '深度', type: 'line', smooth: true, data: props.records.map((r) => r.depthScore) },
      {
        name: '自信',
        type: 'line',
        smooth: true,
        data: props.records.map((r) => r.confidenceScore),
      },
    ],
  })
}

onMounted(() => {
  render()
  window.addEventListener('resize', () => chart?.resize())
})

onUnmounted(() => {
  chart?.dispose()
})

watch(() => props.records, render, { deep: true })
</script>
