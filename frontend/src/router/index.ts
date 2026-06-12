import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import * as interviewApi from '@/api/interview'
import { getAccessToken } from '@/utils/request'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', name: 'home', component: () => import('@/pages/home/HomePage.vue') },
      {
        path: 'interview/select',
        name: 'interview-select',
        component: () => import('@/pages/interview/PositionSelectPage.vue'),
      },
      {
        path: 'interview/:sessionId',
        name: 'interview-room',
        component: () => import('@/pages/interview/InterviewRoomPage.vue'),
        meta: { allowsActiveInterview: true },
      },
      {
        path: 'interview/:sessionId/end',
        name: 'interview-end',
        component: () => import('@/pages/interview/InterviewEndPage.vue'),
        meta: { allowsActiveInterview: true, hideInterviewChrome: true },
      },
      { path: 'reports', name: 'reports', component: () => import('@/pages/report/ReportListPage.vue') },
      {
        path: 'reports/:reportId',
        name: 'report-detail',
        component: () => import('@/pages/report/ReportDetailPage.vue'),
      },
      { path: 'growth', name: 'growth', component: () => import('@/pages/growth/GrowthPage.vue') },
      {
        path: 'knowledge',
        name: 'knowledge',
        component: () => import('@/pages/knowledge/KnowledgePage.vue'),
      },
      {
        path: 'resources',
        name: 'resources',
        component: () => import('@/pages/resources/ResourcesPage.vue'),
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/pages/profile/ProfilePage.vue'),
      },
    ],
  },
  {
    path: '/auth',
    component: () => import('@/layouts/AuthLayout.vue'),
    meta: { guestOnly: true },
    children: [
      { path: 'login', name: 'login', component: () => import('@/pages/auth/LoginPage.vue') },
      {
        path: 'register',
        name: 'register',
        component: () => import('@/pages/auth/RegisterPage.vue'),
      },
    ],
  },
  { path: '/login', redirect: '/auth/login' },
  { path: '/register', redirect: '/auth/register' },
  {
    path: '/share/:token',
    name: 'shared-report',
    component: () => import('@/pages/report/SharedReportPage.vue'),
    meta: { guestOnly: false },
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      {
        path: '',
        name: 'admin-dashboard',
        component: () => import('@/pages/admin/DashboardPage.vue'),
      },
      {
        path: 'positions',
        name: 'admin-positions',
        component: () => import('@/pages/admin/PositionsPage.vue'),
      },
      {
        path: 'questions',
        name: 'admin-questions',
        component: () => import('@/pages/admin/QuestionsPage.vue'),
      },
      { path: 'kb', name: 'admin-kb', component: () => import('@/pages/admin/KbPage.vue') },
      {
        path: 'ai-config',
        name: 'admin-ai-config',
        component: () => import('@/pages/admin/AiConfigPage.vue'),
      },
      { path: 'users', name: 'admin-users', component: () => import('@/pages/admin/UsersPage.vue') },
      {
        path: 'resources',
        name: 'admin-resources',
        component: () => import('@/pages/admin/ResourcesPage.vue'),
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to, _from, next) => {
  const auth = useAuthStore()
  const hasToken = !!getAccessToken()

  if (to.meta.guestOnly && hasToken) {
    return next({ name: 'home' })
  }

  if (to.meta.requiresAuth && !hasToken) {
    return next({ name: 'login', query: { redirect: to.fullPath } })
  }

  if (to.meta.requiresAdmin) {
    if (!hasToken) return next({ name: 'login' })
    if (!auth.userInfo?.role) await auth.fetchProfile()
    if (!auth.isAdmin) return next({ name: 'home' })
  }

  if (to.meta.requiresAuth && hasToken && !to.meta.allowsActiveInterview) {
    try {
      const active = await interviewApi.getActiveInterview()
      if (active.active && active.sessionId) {
        return next({ name: 'interview-room', params: { sessionId: String(active.sessionId) } })
      }
    } catch {
      /* allow navigation if active-session lookup is temporarily unavailable */
    }
  }

  next()
})

export default router
