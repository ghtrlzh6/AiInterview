<template>
  <el-card v-loading="loading">
    <template #header>
      <div class="flex justify-between">
        <span>AI 服务配置</span>
        <el-button @click="testConnection">测试连通性</el-button>
      </div>
    </template>
    <el-form label-width="160px" class="max-w-2xl">
      <el-form-item v-for="(_val, key) in config" :key="key" :label="String(key)">
        <el-input v-model="config[key]" />
      </el-form-item>
    </el-form>
    <el-button type="primary" @click="save">保存配置</el-button>
  </el-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as adminApi from '@/api/admin'

const loading = ref(false)
const config = ref<Record<string, string>>({})

async function load() {
  loading.value = true
  try {
    const data = await adminApi.adminGetAiConfig()
    config.value = Object.fromEntries(
      Object.entries(data).map(([k, v]) => [k, String(v ?? '')]),
    )
  } finally {
    loading.value = false
  }
}

async function save() {
  await adminApi.adminUpdateAiConfig(config.value)
  ElMessage.success('配置已保存')
}

async function testConnection() {
  loading.value = true
  try {
    await adminApi.adminTestAiConfig()
    ElMessage.success('LLM 连接正常')
  } catch {
    ElMessage.error('连接失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
