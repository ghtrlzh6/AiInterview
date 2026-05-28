<template>
  <el-card>
    <template #header>
      <div class="flex justify-between items-center flex-wrap gap-2">
        <span>题库管理</span>
        <div class="flex gap-2">
          <el-button @click="showGen = true">AI 生成题目</el-button>
          <el-button type="primary" @click="openDialog()">新增题目</el-button>
        </div>
      </div>
    </template>

    <el-form :inline="true" class="mb-4">
      <el-form-item>
        <el-input v-model="filters.positionCode" placeholder="岗位编码" clearable />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="题干" show-overflow-tooltip />
      <el-table-column prop="positionCode" label="岗位" width="120" />
      <el-table-column prop="questionType" label="题型" width="120" />
      <el-table-column prop="difficulty" label="难度" width="70" />
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button type="primary" link @click="openDialog(row)">编辑</el-button>
          <el-button type="danger" link @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" title="题目" width="640px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="岗位编码">
          <el-input v-model="form.positionCode" />
        </el-form-item>
        <el-form-item label="题型">
          <el-input v-model="form.questionType" placeholder="TECH_KNOWLEDGE / SCENARIO / ..." />
        </el-form-item>
        <el-form-item label="难度">
          <el-input-number v-model="form.difficulty" :min="1" :max="3" />
        </el-form-item>
        <el-form-item label="题干">
          <el-input v-model="form.title" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showGen" title="AI 生成题目" width="480px">
      <el-form :model="genForm" label-width="100px">
        <el-form-item label="岗位">
          <el-input v-model="genForm.positionCode" />
        </el-form-item>
        <el-form-item label="知识模块ID">
          <el-input-number v-model="genForm.kbModuleId" />
        </el-form-item>
        <el-form-item label="数量">
          <el-input-number v-model="genForm.count" :min="1" :max="10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showGen = false">取消</el-button>
        <el-button type="primary" :loading="genLoading" @click="generate">生成</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as adminApi from '@/api/admin'

const loading = ref(false)
const list = ref<Record<string, unknown>[]>([])
const filters = reactive({ positionCode: '' })
const visible = ref(false)
const form = reactive<Record<string, unknown>>({})
const showGen = ref(false)
const genLoading = ref(false)
const genForm = reactive({ positionCode: '', kbModuleId: 1, count: 3 })

async function load() {
  loading.value = true
  try {
    const res = await adminApi.adminListQuestions(filters)
    list.value = res.list
  } finally {
    loading.value = false
  }
}

function openDialog(row?: Record<string, unknown>) {
  Object.assign(form, row || { positionCode: '', questionType: 'TECH_KNOWLEDGE', difficulty: 2, title: '' })
  visible.value = true
}

async function save() {
  const id = form.id as number | undefined
  if (id) await adminApi.adminUpdateQuestion(id, form)
  else await adminApi.adminCreateQuestion(form)
  ElMessage.success('保存成功')
  visible.value = false
  load()
}

async function remove(id: number) {
  await ElMessageBox.confirm('确定删除？')
  await adminApi.adminDeleteQuestion(id)
  load()
}

async function generate() {
  genLoading.value = true
  try {
    await adminApi.adminGenerateQuestions(genForm)
    ElMessage.success('已提交生成任务')
    showGen.value = false
    load()
  } finally {
    genLoading.value = false
  }
}

onMounted(load)
</script>
