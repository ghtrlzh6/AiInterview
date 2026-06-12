<template>
  <el-container class="min-h-screen">
    <el-aside width="220px" class="bg-slate-900 text-white">
      <div class="p-5 font-bold text-lg border-b border-slate-700">管理后台</div>
      <el-menu
        :default-active="route.path"
        router
        background-color="#0f172a"
        text-color="#94a3b8"
        active-text-color="#fff"
        class="border-0"
      >
        <el-menu-item index="/admin">
          <el-icon><DataAnalysis /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/admin/positions">
          <el-icon><Briefcase /></el-icon>
          <span>岗位管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/questions">
          <el-icon><EditPen /></el-icon>
          <span>题库管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/kb">
          <el-icon><FolderOpened /></el-icon>
          <span>知识库</span>
        </el-menu-item>
        <el-menu-item index="/admin/ai-config">
          <el-icon><Cpu /></el-icon>
          <span>AI 配置</span>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/resources">
          <el-icon><Reading /></el-icon>
          <span>学习资源</span>
        </el-menu-item>
      </el-menu>
      <div class="p-4 mt-auto">
        <el-button type="primary" link class="!text-slate-400" @click="router.push('/')">
          返回前台
        </el-button>
      </div>
    </el-aside>
    <el-container>
      <el-header class="bg-white border-b flex items-center justify-between h-14 px-6">
        <span class="font-medium text-slate-700">{{ pageTitle }}</span>
        <span class="text-sm text-slate-500">{{ auth.userInfo?.nickname }}</span>
      </el-header>
      <el-main class="bg-slate-100 p-6">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const titles: Record<string, string> = {
  '/admin': '数据统计',
  '/admin/positions': '岗位管理',
  '/admin/questions': '题库管理',
  '/admin/kb': '知识库管理',
  '/admin/ai-config': 'AI 配置',
  '/admin/users': '用户管理',
  '/admin/resources': '学习资源管理',
}

const pageTitle = computed(() => titles[route.path] || '管理后台')
</script>
