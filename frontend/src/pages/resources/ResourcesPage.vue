<template>
  <div class="max-w-5xl mx-auto space-y-6">
    <h1 class="text-2xl font-bold">我的学习资源</h1>
    <el-card v-if="reportIdFromQuery">
      <template #header>基于报告的推荐</template>
      <div v-loading="recLoading">
        <el-empty v-if="!recommendations.length" description="暂无推荐" />
        <div v-for="item in recommendations" :key="item.recommendationId" class="border-b py-4 last:border-0">
          <h3 class="font-semibold">{{ item.resource.title }}</h3>
          <p class="text-sm text-slate-500 mt-1">{{ item.resource.description }}</p>
          <p v-if="item.reason" class="text-xs text-indigo-600 mt-1">推荐原因：{{ item.reason }}</p>
          <div class="mt-2 flex gap-2">
            <el-button v-if="item.resource.url" type="primary" link :href="item.resource.url" target="_blank">
              打开链接
            </el-button>
            <el-button size="small" @click="feedback(item.recommendationId, true)">有帮助</el-button>
            <el-button size="small" @click="feedback(item.recommendationId, false)">没帮助</el-button>
          </div>
        </div>
      </div>
    </el-card>

    <el-card>
      <template #header>资源搜索</template>
      <el-form :inline="true" class="mb-4">
        <el-form-item>
          <el-input v-model="keyword" placeholder="主题关键词" clearable />
        </el-form-item>

        <el-form-item>
          <el-select
            v-model="resourceType"
            placeholder="资源类型"
            clearable
            style="width: 140px"
          >
            <el-option label="视频" value="VIDEO" />
            <el-option label="课程" value="COURSE" />
            <el-option label="文章" value="ARTICLE" />
            <el-option label="书籍" value="BOOK" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="search">搜索</el-button>
        </el-form-item>

      </el-form>
      <el-row :gutter="20" v-loading="searchLoading">

        <el-col
          :span="8"
          v-for="item in searchResults"
          :key="item.id"
        >

          <el-card shadow="hover" class="mb-4">

            <div class="flex justify-between items-center">

              <span class="font-semibold">
                {{ item.title }}
              </span>

              <el-tag
                size="small"
                :type="typeTagType[item.resourceType]"
              >
                {{ typeMap[item.resourceType] }}
              </el-tag>

            </div>

            <p class="text-slate-500 text-sm mt-2 line-clamp-3">
              {{ item.description }}
            </p>

            <div class="mt-3 flex justify-between items-center">

              <el-tag size="small">
                {{ item.topic }}
              </el-tag>

              <el-link
                v-if="item.url"
                :href="item.url"
                target="_blank"
                type="primary"
              >
                查看 →
              </el-link>

            </div>

          </el-card>

        </el-col>

      </el-row>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as resourceApi from '@/api/resource'
import type { LearningResource } from '@/types'
import type { RecommendationItem } from '@/api/resource'

const route = useRoute()
const reportIdFromQuery = ref(Number(route.query.reportId) || 0)
const recLoading = ref(false)
const recommendations = ref<RecommendationItem[]>([])
const keyword = ref('')
const searchLoading = ref(false)
const searchResults = ref<LearningResource[]>([])
const resourceType = ref('')
const typeMap: Record<string, string> = {
  VIDEO: '视频课程',
  COURSE: '在线课程',
  ARTICLE: '技术文章',
  BOOK: '电子书'
}

const typeTagType: Record<string, any> = {
  VIDEO: 'danger',
  COURSE: 'success',
  ARTICLE: 'primary',
  BOOK: 'warning'
}

async function loadRecommendations() {
  if (!reportIdFromQuery.value) return
  recLoading.value = true
  try {
    const res = await resourceApi.getRecommendations(reportIdFromQuery.value)
    recommendations.value = res.recommendations
  } finally {
    recLoading.value = false
  }
}

async function search() {
  searchLoading.value = true
  try {
    const res = await resourceApi.searchResources({
      keyword: keyword.value,
      type: resourceType.value ,
      page: 1,
      size: 20
    })
    searchResults.value = res.list
  } finally {
    searchLoading.value = false
  }
}

async function feedback(id: number, helpful: boolean) {
  await resourceApi.feedbackRecommendation(id, helpful)
  ElMessage.success('感谢反馈')
}

onMounted(() => {
  loadRecommendations()
  search()
})

</script>


