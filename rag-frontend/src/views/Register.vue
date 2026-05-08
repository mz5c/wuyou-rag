<template>
  <div class="register-page">
    <div class="register-brand">
      <div class="register-brand__content">
        <div class="register-brand__logo">
          <div class="register-brand__logo-icon">W</div>
          <span>Wuyou RAG</span>
        </div>
        <h1 class="register-brand__title">企业级智能知识库</h1>
        <p class="register-brand__desc">基于 RAG 技术，让您的文档资产<br>转化为可对话的智慧</p>
        <p class="register-brand__copyright">&copy; 2026 Wuyou RAG</p>
      </div>
    </div>
    <div class="register-form">
      <div class="register-form__inner">
        <h2 class="register-form__title">注册账号</h2>
        <p class="register-form__subtitle">创建您的账户</p>
        <div class="register-form__fields">
          <WInput v-model="form.username" placeholder="用户名（2-32位字母、数字或下划线）" prefix-icon="user" />
          <WInput v-model="form.nickname" placeholder="昵称" prefix-icon="user" />
          <WInput v-model="form.password" type="password" placeholder="密码（6-32位）" prefix-icon="lock" />
          <WInput v-model="form.confirmPassword" type="password" placeholder="确认密码" prefix-icon="lock" />
          <p v-if="errorMsg" class="register-form__error">{{ errorMsg }}</p>
          <WButton variant="primary" block :loading="loading" @click="handleRegister">注 册</WButton>
        </div>
        <p class="register-form__switch">已有账号？<router-link to="/login">立即登录</router-link></p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { register, login } from '../api/auth'
import { useAuth } from '../store/auth'

const router = useRouter()
const { setUser } = useAuth()
const loading = ref(false)
const errorMsg = ref('')

const form = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: ''
})

async function handleRegister() {
  const username = form.username.trim()
  const nickname = form.nickname.trim()
  const password = form.password
  const confirmPassword = form.confirmPassword

  if (!username || !nickname || !password || !confirmPassword) {
    errorMsg.value = '请填写所有字段'
    return
  }
  if (!/^[a-zA-Z0-9_]{2,32}$/.test(username)) {
    errorMsg.value = '用户名长度为2-32位，只能包含字母、数字和下划线'
    return
  }
  if (nickname.length < 1 || nickname.length > 32) {
    errorMsg.value = '昵称长度为1-32位'
    return
  }
  if (password.length < 6 || password.length > 32) {
    errorMsg.value = '密码长度为6-32位'
    return
  }
  if (password !== confirmPassword) {
    errorMsg.value = '两次输入的密码不一致'
    return
  }

  loading.value = true
  errorMsg.value = ''
  try {
    await register({ username, password, nickname })
    // Auto-login after successful registration
    const loginRes = await login(username, password)
    const data = loginRes.data
    const token = data.token || data.data?.token
    if (!token) {
      errorMsg.value = '注册成功，自动登录失败，请手动登录'
      router.push('/login')
      return
    }
    setUser({
      token,
      userId: data.userId || data.data?.userId,
      username: data.username || data.data?.username || username,
      nickname: data.nickname || data.data?.nickname || nickname,
      role: data.role || data.data?.role || ''
    })
    await router.push('/chat')
  } catch {
    errorMsg.value = '注册失败，请重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  display: flex;
  min-height: 100vh;
}

.register-brand {
  flex: 1;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #134e4a 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}
.register-brand::before {
  content: '';
  position: absolute;
  width: 600px;
  height: 600px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(13,148,136,0.15), transparent);
  top: -200px;
  right: -200px;
}
.register-brand::after {
  content: '';
  position: absolute;
  width: 400px;
  height: 400px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(13,148,136,0.1), transparent);
  bottom: -100px;
  left: -100px;
}
.register-brand__content {
  text-align: center;
  z-index: 1;
}
.register-brand__logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 32px;
}
.register-brand__logo-icon {
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
.register-brand__logo span {
  color: #fff;
  font-size: 22px;
  font-weight: 700;
  font-family: var(--font-heading);
}
.register-brand__title {
  font-size: 32px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 12px;
  font-family: var(--font-heading);
}
.register-brand__desc {
  font-size: 15px;
  color: rgba(255,255,255,0.5);
  line-height: 1.7;
  margin: 0 0 48px;
}
.register-brand__copyright {
  font-size: 12px;
  color: rgba(255,255,255,0.25);
}

.register-form {
  width: 440px;
  min-width: 440px;
  background: var(--color-surface);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}
.register-form__inner {
  width: 100%;
  max-width: 340px;
}
.register-form__title {
  font-size: 26px;
  font-weight: 700;
  color: var(--color-text);
  margin: 0 0 6px;
  font-family: var(--font-heading);
}
.register-form__subtitle {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 0 0 36px;
}
.register-form__fields {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.register-form__error {
  font-size: var(--font-size-sm);
  color: var(--color-danger);
  margin: 0;
}
.register-form__switch {
  text-align: center;
  margin-top: 24px;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}
.register-form__switch a {
  color: var(--color-primary-600);
  font-weight: 500;
  text-decoration: none;
}
.register-form__switch a:hover {
  color: var(--color-primary-500);
}
</style>
