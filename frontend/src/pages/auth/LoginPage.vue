<template>
  <div>
    <h2 class="text-xl font-semibold text-slate-800 mb-6 text-center">登录</h2>
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="onSubmit">
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" placeholder="请输入用户名" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
      </el-form-item>
      <el-button type="primary" class="w-full" :loading="auth.loading" native-type="submit">
        登录
      </el-button>
    </el-form>
    <p class="text-center text-sm text-slate-500 mt-4">
      还没有账号？
      <router-link to="/auth/register" class="text-brand-600">立即注册</router-link>
    </p>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const formRef = ref<FormInstance>()

const form = reactive({ username: '', password: '' })
const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function onSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  await auth.login(form)
  const redirect = (route.query.redirect as string) || '/'
  router.push(redirect)
}
</script>
