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
          </el-form-item>
        </el-col>
      </el-row>
      <el-divider />
      <el-form-item label="简历项目深挖">
        <div class="w-full space-y-3">
          <el-upload
            :auto-upload="false"
            :show-file-list="false"
            accept="application/pdf"
            :on-change="handleResumeFile"
          >
            <el-button :icon="Upload" :loading="resumeUploading">上传 PDF 简历</el-button>
          </el-upload>
          <div v-if="resumeStatus" class="text-sm text-slate-600">
            {{ resumeStatus.fileName || '简历' }}：
            <el-tag size="small" :type="resumeTagType">{{ resumeStatus.parseStatus }}</el-tag>
            <span v-if="resumeStatus.remark" class="ml-2 text-red-500">{{ resumeStatus.remark }}</span>
          </div>
          <el-radio-group v-if="resumeProjects.length" v-model="selectedResumeId" class="resume-list">
            <el-radio :value="0">不使用简历</el-radio>
            <el-radio :value="resumeStatus?.resumeId || 0">
              使用已解析简历（{{ resumeProjects[0]?.projectName }}）
            </el-radio>
          </el-radio-group>
          <div v-if="resumeProjects.length" class="rounded border border-slate-200 bg-slate-50 p-3 text-sm text-slate-600">
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
        v-for="pos in positions"
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
const selected = ref('')
const questionCount = ref(8)
const resumeUploading = ref(false)
const resumeStatus = ref<ResumeStatus | null>(null)
const resumeProjects = ref<ResumeProject[]>([])
const selectedResumeId = ref(0)

const resumeTagType = computed(() => {
  if (resumeStatus.value?.parseStatus === 'SUCCESS') return 'success'
  if (resumeStatus.value?.parseStatus === 'FAILED') return 'danger'
  return 'warning'
})

onMounted(async () => {
  try {
    positions.value = await positionApi.listPositions()
  } finally {
    loading.value = false
  }
})

async function startInterview() {
  if (!selected.value) return
  starting.value = true
  interview.reset()
  try {
    const res = await interview.start(selected.value, {
      questionCount: questionCount.value,
      resumeSnapshotId: selectedResumeId.value || undefined,
    })
    router.push({ name: 'interview-room', params: { sessionId: String(res.sessionId) } })
  } catch {
    ElMessage.error('创建面试失败')
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
    await pollResume(resumeStatus.value.resumeId)
  } catch {
    ElMessage.error('简历上传失败')
  } finally {
    resumeUploading.value = false
  }
}

async function pollResume(resumeId: number) {
  for (let i = 0; i < 20; i += 1) {
    resumeStatus.value = await resumeApi.getResumeStatus(resumeId)
    if (resumeStatus.value.parseStatus === 'SUCCESS') {
      resumeProjects.value = await resumeApi.listResumeProjects(resumeId)
      selectedResumeId.value = resumeId
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
</script>

<style scoped>
.resume-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
</style>
