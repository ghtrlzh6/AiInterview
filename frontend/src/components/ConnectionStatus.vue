<template>
  <div
    class="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-medium"
    :class="connected ? 'bg-emerald-50 text-emerald-700' : 'bg-red-50 text-red-700'"
  >
    <span
      class="w-2 h-2 rounded-full"
      :class="connected ? 'bg-emerald-500 animate-pulse' : 'bg-red-500'"
    />
    {{ connected ? '已连接' : '连接异常' }}
  </div>
</template>

<script setup lang="ts">
defineProps<{ connected: boolean }>()
</script>
