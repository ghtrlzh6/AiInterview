<template>
  <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-indigo-50 via-white to-slate-100 p-6">
    <div class="w-full max-w-md" style="max-width: min(28rem, calc(100vw - 2rem));">
      <div class="text-center mb-8">
        <h1 class="text-3xl font-bold text-slate-800">AI 模拟面试</h1>
        <p class="text-slate-500 mt-2">提升面试能力，斩获心仪 Offer</p>
      </div>
      <div class="bg-white/80 backdrop-blur rounded-2xl shadow-xl border border-slate-100 p-5 sm:p-8">
        <router-view />
      </div>
    </div>
  </div>
</template>
