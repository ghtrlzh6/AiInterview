<template>
  <div ref="chartRef" class="w-full" :style="{ height: height }" />
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'
import type { ReportScores } from '@/types'

const props = withDefaults(
  defineProps<{
    scores: ReportScores
    height?: string
  }>(),
  { height: '320px' },
)

const chartRef = ref<HTMLElement | null>(null)
let chart: echarts.ECharts | null = null

const dimensionLabels: Record<keyof ReportScores, string> = {
  tech: '技术正确性',
  expression: '语言表达',
  logic: '逻辑严谨',
  depth: '知识深度',
  confidence: '自信度',
}

function render() {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)

  const keys = Object.keys(dimensionLabels) as (keyof ReportScores)[]
  const values = keys.map((k) => props.scores[k])

  chart.setOption({
    tooltip: {},
    radar: {
      indicator: keys.map((k) => ({ name: dimensionLabels[k], max: 100 })),
      splitArea: { areaStyle: { color: ['#f8fafc', '#fff'] } },
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            value: values,
            name: '能力维度',
            areaStyle: { color: 'rgba(79, 70, 229, 0.25)' },
            lineStyle: { color: '#4f46e5' },
            itemStyle: { color: '#4f46e5' },
          },
        ],
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
  chart = null
})

watch(() => props.scores, render, { deep: true })
</script>
