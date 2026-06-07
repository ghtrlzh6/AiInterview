<template>
  <div class="max-w-3xl mx-auto space-y-6">
    <div>
      <h1 class="text-2xl font-bold text-slate-800">个人档案</h1>
      <p class="text-sm text-slate-500 mt-1">完善资料、上传头像与简历，系统将更好地为你推荐岗位与学习资源</p>
    </div>

    <el-card v-loading="loading">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="onSubmit"
      >
        <el-form-item label="头像">
          <div class="flex items-center gap-6">
            <el-avatar :size="88" :src="avatarPreview" class="bg-indigo-100 text-indigo-600 text-2xl">
              {{ form.nickname?.charAt(0) || 'U' }}
            </el-avatar>
            <div class="space-y-2">
              <el-upload
                :show-file-list="false"
                accept="image/jpeg,image/png,image/webp,image/gif"
                :before-upload="beforeAvatarUpload"
                :http-request="handleAvatarUpload"
              >
                <el-button type="primary" plain :loading="avatarUploading">更换头像</el-button>
              </el-upload>
              <p class="text-xs text-slate-500">支持 JPG / PNG / WEBP / GIF，不超过 2MB</p>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="用户名">
          <el-input v-model="form.username" disabled />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="显示在首页与面试中的名称" maxlength="32" />
        </el-form-item>
        <el-form-item label="学校" prop="school">
          <el-input v-model="form.school" placeholder="例如：某某大学" maxlength="64" />
        </el-form-item>
        <el-form-item label="专业" prop="major">
          <el-input v-model="form.major" placeholder="例如：计算机科学与技术" maxlength="64" />
        </el-form-item>

        <el-divider content-position="left">简历提取信息</el-divider>
        <p class="text-xs text-slate-500 -mt-2 mb-4">上传 PDF 简历后会按「教育经历 / 个人能力 / 项目经历 / 实习经历」等标题自动填入，可手动修改后保存</p>

        <el-form-item label="教育经历">
          <el-input
            v-model="form.educationExperience"
            type="textarea"
            :rows="4"
            placeholder="上传简历后自动提取，或手动填写"
          />
        </el-form-item>
        <el-form-item label="个人能力">
          <el-input
            v-model="form.personalSkills"
            type="textarea"
            :rows="4"
            placeholder="技能、工具、语言等"
          />
        </el-form-item>
        <el-form-item label="项目经历">
          <el-input
            v-model="form.projectExperience"
            type="textarea"
            :rows="5"
            placeholder="项目描述、职责与成果"
          />
        </el-form-item>
        <el-form-item label="实习 / 工作经历">
          <el-input
            v-model="form.internshipExperience"
            type="textarea"
            :rows="4"
            placeholder="实习或全职工作描述"
          />
        </el-form-item>

        <el-form-item label="目标岗位" prop="targetPositionCode">
          <el-select
            v-model="form.targetPositionCode"
            placeholder="选择你准备面试的岗位方向"
            clearable
            class="w-full"
          >
            <el-option v-for="p in positions" :key="p.code" :label="p.name" :value="p.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="累计面试">
          <el-input :model-value="String(form.totalInterviews ?? 0)" disabled>
            <template #append>次</template>
          </el-input>
        </el-form-item>
        <div class="flex gap-3 pt-2">
          <el-button type="primary" :loading="saving" native-type="submit">保存修改</el-button>
          <el-button @click="resetForm">重置</el-button>
        </div>
      </el-form>
    </el-card>

    <el-card v-loading="resumeLoading">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-semibold">PDF 简历</span>
          <el-tag v-if="resume.parseStatus" :type="resumeStatusType">{{ resumeStatusLabel }}</el-tag>
        </div>
      </template>

      <el-upload
        drag
        accept=".pdf,application/pdf"
        :show-file-list="false"
        :before-upload="beforeResumeUpload"
        :http-request="handleResumeUpload"
        :disabled="resumeUploading"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">
          将 PDF 简历拖到此处，或 <em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip text-slate-500">仅支持 PDF，上传后自动解析文本与项目经历</div>
        </template>
      </el-upload>

      <div v-if="resume.fileName" class="mt-4 text-sm text-slate-600">
        当前文件：<span class="font-medium">{{ resume.fileName }}</span>
        <span v-if="resume.createdAt" class="text-slate-400 ml-2">{{ formatTime(resume.createdAt) }}</span>
      </div>

      <el-alert
        v-if="resume.parseStatus === 'FAILED'"
        class="mt-4"
        type="error"
        :closable="false"
        :title="resume.remark || '简历解析失败，请检查 PDF 是否可复制文本'"
      />

      <div v-if="resume.parseStatus === 'PENDING'" class="mt-4 flex items-center gap-2 text-amber-600 text-sm">
        <el-icon class="is-loading"><Loading /></el-icon>
        正在解析简历，请稍候…
      </div>

      <template v-if="resume.parseStatus === 'SUCCESS'">
        <div v-if="resumeProjects.length" class="mt-6 space-y-3">
          <h3 class="font-semibold text-slate-800">识别到的项目经历</h3>
          <div
            v-for="project in resumeProjects"
            :key="project.id"
            class="rounded-lg border border-slate-200 p-4 bg-slate-50"
          >
            <div class="font-medium text-slate-800">{{ project.projectName }}</div>
            <p v-if="project.summaryMd" class="text-sm text-slate-600 mt-2 whitespace-pre-wrap">
              {{ project.summaryMd }}
            </p>
            <div v-if="project.techStackTokens?.length" class="flex flex-wrap gap-2 mt-3">
              <el-tag v-for="tech in project.techStackTokens" :key="tech" size="small" type="info">
                {{ tech }}
              </el-tag>
            </div>
          </div>
        </div>

        <div v-if="resume.resumeTextPreview" class="mt-6">
          <h3 class="font-semibold text-slate-800 mb-2">解析全文预览</h3>
          <el-input
            :model-value="resume.resumeTextPreview"
            type="textarea"
            :rows="12"
            readonly
            class="font-mono text-sm"
          />
        </div>
      </template>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, type FormInstance, type FormRules, type UploadRequestOptions } from 'element-plus'
import * as userApi from '@/api/user'
import * as positionApi from '@/api/position'
import * as resumeApi from '@/api/resume'
import { useAuthStore } from '@/stores/auth'
import { resolveUploadUrl } from '@/utils/upload'
import type { Position } from '@/types'
import type { ResumeProjectItem, ResumeStatus } from '@/api/resume'

const auth = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)
const avatarUploading = ref(false)
const resumeLoading = ref(false)
const resumeUploading = ref(false)
const positions = ref<Position[]>([])
const resume = ref<ResumeStatus>({})
const resumeProjects = ref<ResumeProjectItem[]>([])
let pollTimer: ReturnType<typeof setInterval> | null = null

const form = reactive({
  username: '',
  nickname: '',
  avatarUrl: '',
  school: '',
  major: '',
  educationExperience: '',
  personalSkills: '',
  projectExperience: '',
  internshipExperience: '',
  targetPositionCode: '',
  totalInterviews: 0,
})

const initialForm = reactive({ ...form })

const avatarPreview = computed(() => resolveUploadUrl(form.avatarUrl))

const rules: FormRules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 1, max: 32, message: '昵称长度 1~32 个字符', trigger: 'blur' },
  ],
}

const STATUS_LABELS: Record<string, string> = {
  PENDING: '解析中',
  SUCCESS: '解析成功',
  FAILED: '解析失败',
}

const STATUS_TYPES: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
  PENDING: 'warning',
  SUCCESS: 'success',
  FAILED: 'danger',
}

const resumeStatusLabel = computed(() =>
  resume.value.parseStatus ? STATUS_LABELS[resume.value.parseStatus] ?? resume.value.parseStatus : '',
)

const resumeStatusType = computed(() =>
  resume.value.parseStatus ? STATUS_TYPES[resume.value.parseStatus] ?? 'info' : 'info',
)

function formatTime(value: string) {
  return value.replace('T', ' ').slice(0, 16)
}

function applyProfile(profile: userApi.UserProfile) {
  form.username = profile.username
  form.nickname = profile.nickname
  form.avatarUrl = profile.avatarUrl ?? ''
  form.school = profile.school ?? ''
  form.major = profile.major ?? ''
  form.educationExperience = profile.educationExperience ?? ''
  form.personalSkills = profile.personalSkills ?? ''
  form.projectExperience = profile.projectExperience ?? ''
  form.internshipExperience = profile.internshipExperience ?? ''
  form.targetPositionCode = profile.targetPositionCode ?? ''
  form.totalInterviews = profile.totalInterviews ?? 0
  Object.assign(initialForm, form)
}

function resetForm() {
  Object.assign(form, initialForm)
  formRef.value?.clearValidate()
}

function beforeAvatarUpload(file: File) {
  const allowed = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']
  if (!allowed.includes(file.type)) {
    ElMessage.error('仅支持 JPG、PNG、WEBP、GIF 格式')
    return false
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error('头像大小不能超过 2MB')
    return false
  }
  return true
}

async function handleAvatarUpload(options: UploadRequestOptions) {
  const file = options.file as File
  avatarUploading.value = true
  try {
    const updated = await userApi.uploadAvatar(file)
    applyProfile(updated)
    await auth.fetchProfile()
    ElMessage.success('头像已更新')
    options.onSuccess?.({})
  } finally {
    avatarUploading.value = false
  }
}

function beforeResumeUpload(file: File) {
  const isPdf = file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf')
  if (!isPdf) {
    ElMessage.error('仅支持 PDF 格式')
    return false
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('PDF 大小不能超过 10MB')
    return false
  }
  return true
}

async function loadResumeProjects(resumeId: number) {
  resumeProjects.value = await resumeApi.getResumeProjects(resumeId)
}

async function refreshProfileFromServer() {
  const profile = await userApi.getMe()
  applyProfile(profile)
}

async function applyResumeStatus(status: ResumeStatus) {
  if (!status.resumeId) {
    resume.value = {}
    resumeProjects.value = []
    return
  }
  resume.value = status
  if (status.parseStatus === 'SUCCESS') {
    await loadResumeProjects(status.resumeId)
    await refreshProfileFromServer()
  } else {
    resumeProjects.value = []
  }
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

function startPolling(resumeId: number) {
  stopPolling()
  pollTimer = setInterval(async () => {
    try {
      const status = await resumeApi.getResumeStatus(resumeId)
      await applyResumeStatus(status)
      if (status.parseStatus === 'SUCCESS') {
        stopPolling()
        ElMessage.success('简历解析完成，已填入基本信息')
      } else if (status.parseStatus === 'FAILED') {
        stopPolling()
        ElMessage.error(status.remark || '简历解析失败')
      }
    } catch {
      stopPolling()
    }
  }, 1500)
}

async function handleResumeUpload(options: UploadRequestOptions) {
  const file = options.file as File
  resumeUploading.value = true
  stopPolling()
  try {
    const result = await resumeApi.uploadResume(file)
    resume.value = {
      resumeId: result.resumeId,
      parseStatus: result.parseStatus,
      fileName: file.name,
    }
    resumeProjects.value = []
    ElMessage.success('简历已上传，正在解析…')
    if (result.parseStatus === 'PENDING') {
      startPolling(result.resumeId)
    } else {
      await applyResumeStatus(await resumeApi.getResumeStatus(result.resumeId))
    }
    options.onSuccess?.({})
  } finally {
    resumeUploading.value = false
  }
}

async function loadResume() {
  resumeLoading.value = true
  try {
    const latest = await resumeApi.getLatestResume()
    await applyResumeStatus(latest)
    if (latest.resumeId && latest.parseStatus === 'PENDING') {
      startPolling(latest.resumeId)
    }
  } catch {
    resume.value = {}
    resumeProjects.value = []
  } finally {
    resumeLoading.value = false
  }
}

async function loadData() {
  loading.value = true
  try {
    const [profile, positionList] = await Promise.all([
      userApi.getMe(),
      positionApi.listPositions(),
    ])
    positions.value = positionList
    applyProfile(profile)
  } finally {
    loading.value = false
  }
}

async function onSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const updated = await userApi.updateMe({
      nickname: form.nickname.trim(),
      school: form.school.trim(),
      major: form.major.trim(),
      educationExperience: form.educationExperience.trim(),
      personalSkills: form.personalSkills.trim(),
      projectExperience: form.projectExperience.trim(),
      internshipExperience: form.internshipExperience.trim(),
      targetPositionCode: form.targetPositionCode || '',
    })
    applyProfile(updated)
    await auth.fetchProfile()
    ElMessage.success('档案已保存')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadData(), loadResume()])
})

onBeforeUnmount(stopPolling)
</script>
