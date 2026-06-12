<template>
  <el-card>
    <template #header>
      <div class="flex justify-between items-center flex-wrap gap-2">
        <span>学习资源管理</span>
        <el-button type="primary" @click="openDialog()">新增资源</el-button>
      </div>
    </template>

    <el-form :inline="true" class="mb-4">
      <el-form-item>
        <el-select v-model="filters.positionCode" placeholder="岗位" clearable style="width: 180px">
          <el-option v-for="p in positions" :key="p.code" :label="p.name" :value="p.code" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" show-overflow-tooltip />
      <el-table-column prop="positionCode" label="岗位" width="130" />
      <el-table-column prop="resourceType" label="类型" width="100" />
      <el-table-column prop="topic" label="主题" width="120" show-overflow-tooltip />
      <el-table-column prop="difficulty" label="难度" width="70" />
      <el-table-column prop="qualityScore" label="质量分" width="80" />
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button type="primary" link @click="openDialog(row)">编辑</el-button>
          <el-button type="danger" link @click="remove(row.id as number)">删除</el-button>
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

    <el-dialog v-model="visible" :title="form.id ? '编辑资源' : '新增资源'" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="岗位" required>
          <el-select v-model="form.positionCode" placeholder="选择岗位" style="width: 100%">
            <el-option v-for="p in positions" :key="p.code" :label="p.name" :value="p.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" required>
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="类型" required>
          <el-select v-model="form.resourceType" style="width: 100%">
            <el-option value="ARTICLE" label="文章" />
            <el-option value="VIDEO" label="视频" />
            <el-option value="COURSE" label="课程" />
            <el-option value="BOOK" label="书籍" />
          </el-select>
        </el-form-item>
        <el-form-item label="链接">
          <el-input v-model="form.url" placeholder="https://" />
        </el-form-item>
        <el-form-item label="主题">
          <el-input v-model="form.topic" />
        </el-form-item>
        <el-form-item label="难度">
          <el-input-number v-model="form.difficulty" :min="1" :max="3" />
        </el-form-item>
        <el-form-item label="质量分">
          <el-input-number v-model="form.qualityScore" :min="1" :max="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as adminApi from '@/api/admin'
import type { Position } from '@/types'

interface ResourceRow {
  id?: number
  positionCode?: string
  title?: string
  description?: string
  resourceType?: string
  url?: string
  topic?: string
  difficulty?: number
  qualityScore?: number
}

const loading = ref(false)
const list = ref<ResourceRow[]>([])
const page = ref(1)
const total = ref(0)
const positions = ref<Position[]>([])
const visible = ref(false)
const filters = reactive({ positionCode: '' })
const form = reactive<ResourceRow>({})

async function loadPositions() {
  positions.value = await adminApi.adminListPositions()
}

async function load() {
  loading.value = true
  try {
    const params: Record<string, unknown> = { page: page.value, size: 20 }
    if (filters.positionCode) params.positionCode = filters.positionCode
    const res = await adminApi.adminListResources(params)
    list.value = res.list as ResourceRow[]
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function openDialog(row?: ResourceRow) {
  Object.assign(
    form,
    row || {
      positionCode: positions.value[0]?.code || '',
      title: '',
      description: '',
      resourceType: 'ARTICLE',
      url: '',
      topic: '',
      difficulty: 2,
      qualityScore: 80,
    },
  )
  visible.value = true
}

async function save() {
  if (!form.title || !form.positionCode || !form.resourceType) {
    ElMessage.warning('请填写岗位、标题和类型')
    return
  }
  if (form.id) {
    await adminApi.adminUpdateResource(form.id, form)
  } else {
    await adminApi.adminCreateResource(form)
  }
  ElMessage.success('保存成功')
  visible.value = false
  load()
}

async function remove(id: number) {
  await ElMessageBox.confirm('确定删除该资源？')
  await adminApi.adminDeleteResource(id)
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  await loadPositions()
  await load()
})
</script>
