<template>
  <div class="max-w-4xl mx-auto">
    <h1 class="text-2xl font-bold text-slate-800 mb-2">选择面试岗位</h1>
    <p class="text-slate-500 mb-8">选择目标岗位，AI 面试官将围绕该岗位技术栈提问</p>

    <el-form label-position="top" class="mb-6 bg-white p-6 rounded-xl border">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="输入方式">
            <el-radio-group v-model="interview.inputMode">
              <el-radio value="TEXT">文字</el-radio>
              <el-radio value="VOICE">语音</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="题目数量">
            <el-slider v-model="questionCount" :min="3" :max="15" show-input />
            <div class="mt-2 text-sm text-slate-500">预计时长：{{ estimatedDuration }}</div>
          </el-form-item>
        </el-col>
      </el-row>
      <el-divider />
      <el-form-item label="简历项目深挖">
        <div class="w-full space-y-3">
          <div v-loading="resumeLoading" class="rounded border border-slate-200 bg-slate-50 p-3">
            <div class="flex flex-wrap items-center justify-between gap-3">
              <div class="text-sm text-slate-600">
                <template v-if="resumeStatus?.resumeId">
                  <span class="font-medium text-slate-800">{{ resumeStatus.fileName || '个人档案简历' }}</span>
                  <el-tag size="small" class="ml-2" :type="resumeTagType">{{ resumeStatusLabel }}</el-tag>
                  <span v-if="resumeStatus.remark" class="ml-2 text-red-500">{{ resumeStatus.remark }}</span>
                </template>
                <template v-else>
                  还没有可用的个人简历
                </template>
              </div>
              <el-button link type="primary" @click="router.push('/profile')">去个人档案</el-button>
            </div>
            <el-radio-group
              v-if="resumeStatus?.parseStatus === 'SUCCESS' && resumeStatus.resumeId"
              v-model="selectedResumeId"
              class="resume-list mt-3"
            >
              <el-radio :value="0">不使用简历</el-radio>
              <el-radio :value="resumeStatus.resumeId">
                使用个人档案简历
              </el-radio>
            </el-radio-group>
          </div>

          <el-upload
            :auto-upload="false"
            :show-file-list="false"
            accept="application/pdf"
            :on-change="handleResumeFile"
          >
            <el-button :icon="Upload" :loading="resumeUploading">临时上传 PDF 简历</el-button>
          </el-upload>
          <div v-if="resumeProjects.length" class="rounded border border-slate-200 bg-slate-50 p-3 text-sm text-slate-600">
            <div class="font-medium text-slate-800 mb-2">识别到的项目经历</div>
            <div v-for="project in resumeProjects" :key="project.id" class="mb-2 last:mb-0">
              <div class="font-medium text-slate-800">{{ project.projectName }}</div>
              <div class="line-clamp-2">{{ project.summaryMd }}</div>
              <div class="mt-1 flex flex-wrap gap-1">
                <el-tag v-for="token in project.techStackTokens || []" :key="token" size="small" effect="plain">
                  {{ token }}
                </el-tag>
              </div>
            </div>
          </div>
        </div>
      </el-form-item>
    </el-form>

    <div v-loading="loading" class="grid gap-4 md:grid-cols-2">
      <el-card
        v-for="pos in pagedPositions"
        :key="pos.code"
        shadow="hover"
        class="cursor-pointer transition hover:ring-2 hover:ring-indigo-400"
        :class="{ 'ring-2 ring-indigo-500': selected === pos.code }"
        @click="selected = pos.code"
      >
        <div class="flex justify-between items-start">
          <div>
            <h3 class="font-semibold text-lg">{{ pos.name }}</h3>
            <p class="text-sm text-slate-500 mt-2 line-clamp-2">{{ pos.description }}</p>
            <div class="flex flex-wrap gap-1 mt-3">
              <el-tag v-for="t in (pos.techStack || []).slice(0, 4)" :key="t" size="small">{{ t }}</el-tag>
            </div>
          </div>
          <el-icon v-if="selected === pos.code" class="text-indigo-500" :size="24">
            <CircleCheckFilled />
          </el-icon>
        </div>
      </el-card>
    </div>

    <div v-if="positions.length > positionPageSize" class="mt-6 flex justify-center">
      <el-pagination
        v-model:current-page="positionPage"
        background
        layout="prev, pager, next"
        :page-size="positionPageSize"
        :total="positions.length"
      />
    </div>

    <div class="mt-8 flex justify-center">
      <el-button
        type="primary"
        size="large"
        :disabled="!selected"
        :loading="starting"
        @click="startInterview"
      >
        进入面试间
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type UploadFile } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import * as positionApi from '@/api/position'
import * as resumeApi from '@/api/resume'
import { useInterviewStore } from '@/stores/interview'
import type { Position, ResumeProject, ResumeStatus } from '@/types'

const router = useRouter()
const interview = useInterviewStore()
const loading = ref(true)
const starting = ref(false)
const positions = ref<Position[]>([])
const positionPage = ref(1)
const positionPageSize = 4
const selected = ref('')
const questionCount = ref(8)
const resumeLoading = ref(false)
const resumeUploading = ref(false)
const resumeStatus = ref<ResumeStatus | null>(null)
const resumeProjects = ref<ResumeProject[]>([])
const selectedResumeId = ref(0)

const estimatedDuration = computed(() => {
  const min = Math.max(10, questionCount.value * 3)
  const max = Math.max(min + 5, questionCount.value * 5)
  return `${min}-${max} 分钟`
})

const pagedPositions = computed(() => {
  const start = (positionPage.value - 1) * positionPageSize
  return positions.value.slice(start, start + positionPageSize)
})

const resumeTagType = computed(() => {
  if (resumeStatus.value?.parseStatus === 'SUCCESS') return 'success'
  if (resumeStatus.value?.parseStatus === 'FAILED') return 'danger'
  return 'warning'
})

const resumeStatusLabel = computed(() => {
  const status = resumeStatus.value?.parseStatus
  if (status === 'SUCCESS') return '解析成功'
  if (status === 'FAILED') return '解析失败'
  if (status === 'PENDING') return '解析中'
  return '未上传'
})

onMounted(async () => {
  try {
    const active = await interview.loadActive()
    if (active?.sessionId) {
      ElMessage.warning('你还有未完成的面试，请先继续或结束当前面试')
      router.replace({ name: 'interview-prepare', params: { sessionId: String(active.sessionId) } })
      return
    }
    await Promise.all([loadPositions(), loadLatestResume()])
  } finally {
    loading.value = false
  }
})

async function loadPositions() {
  positions.value = await positionApi.listPositions()
  positionPage.value = 1
}

async function loadLatestResume() {
  resumeLoading.value = true
  resumeProjects.value = []
  selectedResumeId.value = 0
  try {
    const latest = await resumeApi.getLatestResume()
    if (!latest.resumeId) {
      resumeStatus.value = null
      return
    }
    resumeStatus.value = latest
    if (latest.parseStatus === 'SUCCESS') {
      await applyParsedResume(latest.resumeId)
    } else if (latest.parseStatus === 'PENDING') {
      await pollResume(latest.resumeId)
    }
  } finally {
    resumeLoading.value = false
  }
}

async function startInterview() {
  if (!selected.value) return
  starting.value = true
  interview.reset()
  try {
    const res = await interview.start(selected.value, {
      questionCount: questionCount.value,
      resumeSnapshotId: selectedResumeId.value || undefined,
    })
    router.push({ name: 'interview-prepare', params: { sessionId: String(res.sessionId) } })
  } catch {
    const active = await interview.loadActive()
    if (active?.sessionId) {
      ElMessage.warning('你还有未完成的面试，请先继续或结束当前面试')
      router.replace({ name: 'interview-prepare', params: { sessionId: String(active.sessionId) } })
    } else {
      ElMessage.error('创建面试失败')
    }
  } finally {
    starting.value = false
  }
}

async function handleResumeFile(file: UploadFile) {
  if (!file.raw) return
  resumeUploading.value = true
  resumeProjects.value = []
  selectedResumeId.value = 0
  try {
    const form = new FormData()
    form.append('file', file.raw)
    resumeStatus.value = await resumeApi.uploadResume(form)
    if (!resumeStatus.value.resumeId) {
      ElMessage.error('简历上传失败，可继续普通面试')
      return
    }
    await pollResume(resumeStatus.value.resumeId)
  } catch {
    /* request interceptor already shows the server error */
  } finally {
    resumeUploading.value = false
  }
}

async function pollResume(resumeId: number) {
  for (let i = 0; i < 20; i += 1) {
    resumeStatus.value = await resumeApi.getResumeStatus(resumeId)
    if (resumeStatus.value.parseStatus === 'SUCCESS') {
      await applyParsedResume(resumeId)
      ElMessage.success('简历解析完成')
      return
    }
    if (resumeStatus.value.parseStatus === 'FAILED') {
      ElMessage.error(resumeStatus.value.remark || '简历解析失败，可继续普通面试')
      return
    }
    await new Promise((resolve) => window.setTimeout(resolve, 1200))
  }
  ElMessage.warning('简历仍在解析中，可稍后刷新或直接开始普通面试')
}

async function applyParsedResume(resumeId: number) {
  resumeProjects.value = await resumeApi.listResumeProjects(resumeId)
  selectedResumeId.value = resumeId
}
</script>

<style scoped>
.resume-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

</style>
