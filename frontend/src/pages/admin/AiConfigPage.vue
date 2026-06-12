<template>
  <el-card v-loading="loading">
    <template #header>
      <div class="flex justify-between items-center">
        <span>AI 服务配置</span>
        <div class="flex gap-2">
          <el-button @click="testConnection">测试连通性</el-button>
          <el-button type="primary" @click="save">保存配置</el-button>
        </div>
      </div>
    </template>

    <el-alert
      v-if="testResult"
      :title="testResult.message"
      :type="testResult.success ? 'success' : 'error'"
      class="mb-4"
      show-icon
      :closable="false"
    >
      <template v-if="testResult.success">
        模型：{{ testResult.model }} · 延迟：{{ testResult.latencyMs }}ms
      </template>
    </el-alert>

    <el-form label-width="180px" class="max-w-3xl">
      <el-form-item
        v-for="item in configItems"
        :key="item.key"
        :label="item.description || item.key"
      >
        <el-input
          v-if="item.sensitive"
          v-model="item.value"
          type="password"
          show-password
          :placeholder="item.masked ? '留空表示不修改' : '请输入 API Key'"
        />
        <el-input v-else v-model="item.value" />
        <div v-if="item.sensitive && item.masked" class="text-xs text-slate-400 mt-1">
          当前已配置（显示为掩码），不修改请保持为空
        </div>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as adminApi from '@/api/admin'

interface ConfigItem {
  key: string
  value: string
  type?: string
  sensitive?: boolean
  description?: string
  masked?: boolean
}

interface TestResult {
  success: boolean
  model: string
  latencyMs: number
  message: string
}

const loading = ref(false)
const configItems = ref<ConfigItem[]>([])
const testResult = ref<TestResult | null>(null)

async function load() {
  loading.value = true
  try {
    const data = await adminApi.adminGetAiConfig()
    configItems.value = data.map((item) => ({
      ...item,
      value: item.sensitive && item.value?.includes('****') ? '' : String(item.value ?? ''),
      masked: !!(item.sensitive && item.value?.includes('****')),
    }))
  } finally {
    loading.value = false
  }
}

async function save() {
  loading.value = true
  try {
    const payload = configItems.value
      .filter((item) => item.value || !item.masked)
      .map((item) => ({ key: item.key, value: item.value }))
    await adminApi.adminUpdateAiConfig(payload)
    ElMessage.success('配置已保存，LLM 参数已热更新生效')
    testResult.value = null
    await load()
  } finally {
    loading.value = false
  }
}

async function testConnection() {
  loading.value = true
  testResult.value = null
  try {
    const result = await adminApi.adminTestAiConfig()
    testResult.value = result
    ElMessage.success(result.message)
  } catch (e) {
    const msg = e instanceof Error ? e.message : '连接失败'
    testResult.value = { success: false, model: '-', latencyMs: 0, message: msg }
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
