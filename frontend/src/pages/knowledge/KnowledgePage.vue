<template>
  <div class="flex gap-4 h-[calc(100vh-8rem)]">
    <el-card class="w-72 shrink-0 overflow-hidden flex flex-col">
      <template #header>知识目录</template>
      <el-tree
        v-loading="treeLoading"
        lazy
        :load="loadTreeNode"
        :props="{ label: 'title', isLeaf: (d: KbTreeNode) => !d.hasChildren }"
        highlight-current
        @node-click="onNodeClick"
      />
    </el-card>

    <el-card class="flex-1 overflow-hidden flex flex-col">
      <template #header>
        <span>{{ currentTitle || '选择左侧知识点' }}</span>
      </template>
      <div v-if="articles.length" class="mb-4 flex flex-wrap gap-2">
        <el-button
          v-for="a in articles"
          :key="a.id"
          :type="selectedArticleId === a.id ? 'primary' : 'default'"
          size="small"
          @click="loadArticle(a.id)"
        >
          {{ a.title }}
        </el-button>
      </div>
      <div v-loading="articleLoading" class="flex-1 overflow-y-auto markdown-body pr-2">
        <div v-if="articleHtml" v-html="articleHtml" />
        <el-empty v-else description="点击目录或文章标题开始阅读" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { marked } from 'marked'
import type Node from 'element-plus/es/components/tree/src/model/node'
import * as kbApi from '@/api/kb'
import type { KbArticle, KbTreeNode } from '@/types'

const treeLoading = ref(false)
const articleLoading = ref(false)
const currentTitle = ref('')
const articles = ref<KbArticle[]>([])
const selectedArticleId = ref<number | null>(null)
const articleHtml = ref('')

async function loadTreeNode(
  node: Node,
  resolve: (data: KbTreeNode[]) => void,
) {
  treeLoading.value = true
  try {
    const parentId = node.level === 0 ? undefined : (node.data as KbTreeNode).id
    const children = await kbApi.getKbTree({ parentId })
    resolve(children)
  } catch {
    resolve([])
  } finally {
    treeLoading.value = false
  }
}

async function onNodeClick(data: KbTreeNode) {
  articleLoading.value = true
  try {
    const detail = await kbApi.getKbNode(data.id)
    currentTitle.value = detail.title
    articles.value = detail.articles || []
    if (detail.articles?.length) {
      await loadArticle(detail.articles[0].id)
    } else {
      articleHtml.value = detail.bodyPreview
        ? (marked.parse(detail.bodyPreview, { async: false }) as string)
        : ''
    }
  } finally {
    articleLoading.value = false
  }
}

async function loadArticle(id: number) {
  selectedArticleId.value = id
  articleLoading.value = true
  try {
    const art = await kbApi.getKbArticle(id)
    currentTitle.value = art.title
    articleHtml.value = marked.parse(art.bodyMarkdown || '', { async: false }) as string
  } finally {
    articleLoading.value = false
  }
}
</script>
