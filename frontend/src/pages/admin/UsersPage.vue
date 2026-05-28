<template>
  <el-card>
    <template #header>
      <div class="flex justify-between">
        <span>用户管理</span>
        <el-input v-model="keyword" placeholder="搜索用户名" style="width: 200px" clearable @clear="load" />
        <el-button type="primary" @click="load">搜索</el-button>
      </div>
    </template>
    <el-table v-loading="loading" :data="list" stripe>
      <el-table-column prop="userId" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column prop="role" label="角色" width="100" />
      <el-table-column prop="totalInterviews" label="面试次数" width="100" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-select
            :model-value="row.role"
            size="small"
            style="width: 100px"
            @change="(v: 'USER' | 'ADMIN') => changeRole(row.userId as number, v)"
          >
            <el-option value="USER" label="USER" />
            <el-option value="ADMIN" label="ADMIN" />
          </el-select>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="page"
      :total="total"
      layout="prev, pager, next"
      class="mt-4"
      @current-change="load"
    />
  </el-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as adminApi from '@/api/admin'

const loading = ref(false)
const list = ref<Record<string, unknown>[]>([])
const page = ref(1)
const total = ref(0)
const keyword = ref('')

async function load() {
  loading.value = true
  try {
    const res = await adminApi.adminListUsers({ page: page.value, size: 20, keyword: keyword.value })
    list.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

async function changeRole(userId: number, role: 'USER' | 'ADMIN') {
  await adminApi.adminUpdateUserRole(userId, role)
  ElMessage.success('角色已更新')
  load()
}

onMounted(load)
</script>
