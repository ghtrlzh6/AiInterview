<template>
  <div>
    <h2 class="text-xl font-semibold text-slate-800 mb-6 text-center">注册</h2>
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="onSubmit">
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" placeholder="4-20 位字母数字下划线" />
      </el-form-item>
      <el-form-item label="昵称" prop="nickname">
        <el-input v-model="form.nickname" placeholder="显示名称" />
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="form.email" placeholder="选填" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="form.password" type="password" show-password placeholder="8-20 位，含字母和数字" />
      </el-form-item>
      <el-button type="primary" class="w-full" :loading="auth.loading" native-type="submit">
        注册并登录
      </el-button>
    </el-form>
    <p class="text-center text-sm text-slate-500 mt-4">
      已有账号？
      <router-link to="/auth/login" class="text-brand-600">去登录</router-link>
    </p>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const formRef = ref<FormInstance>()

const form = reactive({
  username: '',
  nickname: '',
  email: '',
  password: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '至少 8 位', trigger: 'blur' },
  ],
}

async function onSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  await auth.register(form)
  router.push('/')
}
</script>
