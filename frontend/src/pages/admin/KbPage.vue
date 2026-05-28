<template>
  <el-row :gutter="16" class="h-[calc(100vh-10rem)]">
    <el-col :span="8">
      <el-card class="h-full flex flex-col">
        <template #header>
          <div class="flex justify-between">
            <span>知识节点</span>
            <el-button size="small" type="primary" @click="openNodeDialog()">新增</el-button>
          </div>
        </template>
        <el-tree
          :data="nodes"
          :props="{ label: 'title', children: 'children' }"
          highlight-current
          @node-click="selectNode"
        />
      </el-card>
    </el-col>
    <el-col :span="16">
      <el-card class="h-full">
        <template #header>
          <div class="flex justify-between">
            <span>{{ selectedNode?.title || '正文编辑' }}</span>
            <div class="flex gap-2">
              <el-button size="small" @click="vectorizeBatch">批量向量化</el-button>
              <el-button size="small" type="primary" @click="openArticleDialog()">新增正文</el-button>
            </div>
          </div>
        </template>
        <el-input
          v-model="articleBody"
          type="textarea"
          :rows="18"
          placeholder="Markdown 正文"
        />
        <div class="mt-4 flex gap-2">
          <el-button type="primary" :disabled="!selectedArticleId" @click="saveArticle">保存正文</el-button>
          <el-button :disabled="!selectedArticleId" @click="vectorizeOne">向量化</el-button>
        </div>
      </el-card>
    </el-col>

    <el-dialog v-model="nodeVisible" title="知识节点" width="480px">
      <el-form :model="nodeForm" label-width="100px">
        <el-form-item label="标题">
          <el-input v-model="nodeForm.title" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="nodeForm.nodeType">
            <el-option value="GROUP" label="分组" />
            <el-option value="TOPIC_POINT" label="知识点" />
          </el-select>
        </el-form-item>
        <el-form-item label="父节点ID">
          <el-input-number v-model="nodeForm.parentId" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="nodeVisible = false">取消</el-button>
        <el-button type="primary" @click="saveNode">保存</el-button>
      </template>
    </el-dialog>
  </el-row>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as adminApi from '@/api/admin'

const nodes = ref<Record<string, unknown>[]>([])
const selectedNode = ref<Record<string, unknown> | null>(null)
const articleBody = ref('')
const selectedArticleId = ref<number | null>(null)
const nodeVisible = ref(false)
const nodeForm = reactive<Record<string, unknown>>({ title: '', nodeType: 'GROUP', parentId: undefined })

async function loadNodes() {
  nodes.value = (await adminApi.adminKbNodes()) as unknown as Record<string, unknown>[]
}

function openNodeDialog() {
  Object.assign(nodeForm, { title: '', nodeType: 'GROUP', parentId: selectedNode.value?.id })
  nodeVisible.value = true
}

async function saveNode() {
  await adminApi.adminCreateKbNode(nodeForm)
  ElMessage.success('节点已保存')
  nodeVisible.value = false
  loadNodes()
}

function selectNode(data: Record<string, unknown>) {
  selectedNode.value = data
}

function openArticleDialog() {
  selectedArticleId.value = null
  articleBody.value = ''
}

async function saveArticle() {
  if (selectedArticleId.value) {
    await adminApi.adminUpdateKbArticle(selectedArticleId.value, { bodyMarkdown: articleBody.value })
  } else {
    const res = (await adminApi.adminCreateKbArticle({
      kbNodeId: selectedNode.value?.id,
      title: '新文章',
      bodyMarkdown: articleBody.value,
    })) as { id?: number }
    selectedArticleId.value = res?.id ?? null
  }
  ElMessage.success('正文已保存')
}

async function vectorizeOne() {
  if (!selectedArticleId.value) return
  await adminApi.adminVectorizeArticle(selectedArticleId.value)
  ElMessage.success('向量化已触发')
}

async function vectorizeBatch() {
  await adminApi.adminVectorizePendingBatch()
  ElMessage.success('批量向量化已触发')
}

onMounted(loadNodes)
</script>
