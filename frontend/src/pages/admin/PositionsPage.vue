<template>
  <el-card>
    <template #header>
      <div class="flex justify-between items-center">
        <span>岗位列表</span>
        <el-button type="primary" @click="openDialog()">新增岗位</el-button>
      </div>
    </template>
    <el-table v-loading="loading" :data="list" stripe>
      <el-table-column prop="code" label="编码" width="140" />
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="description" label="描述" show-overflow-tooltip />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button type="primary" link @click="openDialog(row)">编辑</el-button>
          <el-button type="danger" link @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" :title="form.id ? '编辑岗位' : '新增岗位'" width="520px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="编码">
          <el-input v-model="form.code" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" />
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

const loading = ref(false)
const list = ref<Position[]>([])
const visible = ref(false)
const form = reactive<Partial<Position>>({})

async function load() {
  loading.value = true
  try {
    list.value = await adminApi.adminListPositions()
  } finally {
    loading.value = false
  }
}

function openDialog(row?: Position) {
  Object.assign(form, row || { code: '', name: '', description: '' })
  visible.value = true
}

async function save() {
  if (form.id) {
    await adminApi.adminUpdatePosition(form.id, form)
  } else {
    await adminApi.adminCreatePosition(form)
  }
  ElMessage.success('保存成功')
  visible.value = false
  load()
}

async function remove(id: number) {
  await ElMessageBox.confirm('确定删除该岗位？')
  await adminApi.adminDeletePosition(id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
