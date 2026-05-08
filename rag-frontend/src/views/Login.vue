<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <h1 class="login-title">RAG 智能知识库</h1>
        <p class="login-subtitle">登录您的账户</p>
      </div>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="0"
        size="large"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="用户名"
            :prefix-icon="User"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            show-password
            :prefix-icon="Lock"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            style="width: 100%"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>
      <div v-if="errorMsg" class="login-error">{{ errorMsg }}</div>
      <p class="auth-link">没有账号？<router-link to="/register">立即注册</router-link></p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { login } from '../api/auth'
import { useAuth } from '../store/auth'

const router = useRouter()
const { setUser } = useAuth()

const formRef = ref(null)
const loading = ref(false)
const errorMsg = ref('')

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

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
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
}
.login-header {
  text-align: center;
  margin-bottom: 32px;
}
.login-title {
  font-size: 24px;
  color: #303133;
  margin: 0 0 8px 0;
}
.login-subtitle {
  font-size: 14px;
  color: #909399;
  margin: 0;
}
.login-error {
  color: #f56c6c;
  font-size: 13px;
  text-align: center;
  margin-top: 12px;
}
.auth-link { text-align: center; margin-top: 16px; font-size: 13px; color: #64748b; }
.auth-link a { color: #2563eb; text-decoration: none; }
.auth-link a:hover { text-decoration: underline; }
</style>
