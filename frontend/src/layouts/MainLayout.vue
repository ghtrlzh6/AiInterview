<template>
  <el-container class="min-h-screen">
    <el-aside
      v-if="!isInterviewLocked"
      width="240px"
      class="bg-white border-r border-slate-200 hidden md:block"
    >
      <div class="p-6 border-b border-slate-100">
        <router-link to="/" class="flex items-center gap-2 no-underline">
          <el-icon :size="28" class="text-brand-600"><Monitor /></el-icon>
          <span class="font-bold text-lg text-slate-800">AI 面试</span>
        </router-link>
      </div>
      <el-menu :default-active="activeMenu" router class="border-0">
        <el-menu-item index="/">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
        </el-menu-item>
        <el-menu-item index="/interview/select">
          <el-icon><VideoCamera /></el-icon>
          <span>开始面试</span>
        </el-menu-item>
        <el-menu-item index="/interviews">
          <el-icon><Tickets /></el-icon>
          <span>所有面试</span>
        </el-menu-item>
        <el-menu-item index="/reports">
          <el-icon><Document /></el-icon>
          <span>评估报告</span>
        </el-menu-item>
        <el-menu-item index="/growth">
          <el-icon><TrendCharts /></el-icon>
          <span>成长曲线</span>
        </el-menu-item>
        <el-menu-item index="/knowledge">
          <el-icon><Reading /></el-icon>
          <span>知识库</span>
        </el-menu-item>
        <el-menu-item index="/resources">
          <el-icon><Collection /></el-icon>
          <span>学习资源</span>
        </el-menu-item>
        <el-menu-item index="/profile">
          <el-icon><User /></el-icon>
          <span>个人档案</span>
        </el-menu-item>
        <el-menu-item v-if="auth.isAdmin" index="/admin">
          <el-icon><Setting /></el-icon>
          <span>管理后台</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header
        v-if="!isInterviewLocked"
        class="bg-white border-b border-slate-200 flex items-center justify-between h-16 px-6"
      >
        <span class="font-medium text-slate-700 md:hidden">AI 模拟面试</span>
        <div class="flex items-center gap-4 ml-auto">
          <router-link to="/profile" class="flex items-center gap-2 no-underline">
            <el-avatar :size="32" :src="avatarUrl" class="bg-teal-50 text-teal-700">
              {{ auth.userInfo?.nickname?.charAt(0) || 'U' }}
            </el-avatar>
            <span class="text-sm text-slate-600 hover:text-brand-600">
              {{ auth.userInfo?.nickname || '用户' }}
            </span>
          </router-link>
          <el-button type="danger" link @click="handleLogout">退出</el-button>
        </div>
      </el-header>
      <el-main class="bg-slate-50" :class="isInterviewLocked ? 'p-2 md:p-3' : 'p-6'">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { resolveUploadUrl } from '@/utils/upload'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const avatarUrl = computed(() => resolveUploadUrl(auth.userInfo?.avatarUrl))
const isInterviewLocked = computed(() => route.name === 'interview-room' || route.name === 'interview-prepare')

const activeMenu = computed(() => {
  if (route.path.startsWith('/admin')) return '/admin'
  if (route.path.startsWith('/interviews')) return '/interviews'
  if (route.path.startsWith('/interview')) return '/interview/select'
  return route.path
})

async function handleLogout() {
  await auth.logout()
  router.push({ name: 'login' })
}
</script>
