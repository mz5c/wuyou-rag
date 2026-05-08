<template>
  <div class="login-page">
    <div class="login-brand">
      <div class="login-brand__content">
        <div class="login-brand__logo">
          <div class="login-brand__logo-icon">W</div>
          <span>Wuyou RAG</span>
        </div>
        <h1 class="login-brand__title">内部智能知识库</h1>
        <p class="login-brand__desc">基于 RAG 技术，让您的文档资产<br>转化为可对话的智慧</p>
        <p class="login-brand__copyright">&copy; 2026 Wuyou RAG</p>
      </div>
    </div>
    <div class="login-form">
      <div class="login-form__inner">
        <h2 class="login-form__title">欢迎回来</h2>
        <p class="login-form__subtitle">登录您的账户</p>
        <div class="login-form__fields">
          <WInput v-model="form.username" placeholder="用户名" prefix-icon="user" />
          <WInput v-model="form.password" type="password" placeholder="密码" prefix-icon="lock" />
          <p v-if="errorMsg" class="login-form__error">{{ errorMsg }}</p>
          <WButton variant="primary" block :loading="loading" @click="handleLogin">登 录</WButton>
        </div>
        <p class="login-form__switch">没有账号？<router-link to="/register">立即注册</router-link></p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../api/auth'
import { useAuth } from '../store/auth'

const router = useRouter()
const { setUser } = useAuth()

const loading = ref(false)
const errorMsg = ref('')

const form = reactive({
  username: '',
  password: ''
})

async function handleLogin() {
  if (!form.username.trim() || !form.password.trim()) {
    errorMsg.value = '请填写用户名和密码'
    return
  }

  loading.value = true
  errorMsg.value = ''

  try {
    const loginRes = await login(form.username, form.password)
    const data = loginRes.data
    const token = data.token || data.data?.token
    if (!token) {
      errorMsg.value = '登录失败: 未获取到 token'
      return
    }

    setUser({
      token,
      userId: data.userId || data.data?.userId,
      username: data.username || data.data?.username || form.username,
      nickname: data.nickname || data.data?.nickname || '',
      role: data.role || data.data?.role || ''
    })

    await router.push('/chat')
  } catch (err) {
    errorMsg.value = err.response?.data?.message || err.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  min-height: 100vh;
}

/* Brand panel */
.login-brand {
  flex: 1;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #134e4a 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}
.login-brand::before {
  content: '';
  position: absolute;
  width: 600px;
  height: 600px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(13,148,136,0.15), transparent);
  top: -200px;
  right: -200px;
}
.login-brand::after {
  content: '';
  position: absolute;
  width: 400px;
  height: 400px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(13,148,136,0.1), transparent);
  bottom: -100px;
  left: -100px;
}
.login-brand__content {
  text-align: center;
  z-index: 1;
}
.login-brand__logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 32px;
}
.login-brand__logo-icon {
  width: 48px;
  height: 48px;
  background: var(--gradient-primary);
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 700;
  font-size: 24px;
}
.login-brand__logo span {
  color: #fff;
  font-size: 22px;
  font-weight: 700;
  font-family: var(--font-heading);
}
.login-brand__title {
  font-size: 32px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 12px;
  font-family: var(--font-heading);
}
.login-brand__desc {
  font-size: 15px;
  color: rgba(255,255,255,0.5);
  line-height: 1.7;
  margin: 0 0 48px;
}
.login-brand__copyright {
  font-size: 12px;
  color: rgba(255,255,255,0.25);
}

/* Form panel */
.login-form {
  width: 440px;
  min-width: 440px;
  background: var(--color-surface);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}
.login-form__inner {
  width: 100%;
  max-width: 340px;
}
.login-form__title {
  font-size: 26px;
  font-weight: 700;
  color: var(--color-text);
  margin: 0 0 6px;
  font-family: var(--font-heading);
}
.login-form__subtitle {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 0 0 36px;
}
.login-form__fields {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.login-form__error {
  font-size: var(--font-size-sm);
  color: var(--color-danger);
  margin: 0;
}
.login-form__switch {
  text-align: center;
  margin-top: 24px;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}
.login-form__switch a {
  color: var(--color-primary-600);
  font-weight: 500;
  text-decoration: none;
}
.login-form__switch a:hover {
  color: var(--color-primary-500);
}
</style>
