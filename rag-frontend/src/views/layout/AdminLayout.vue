<template>
  <div class="admin-layout">
    <div class="admin-sidebar">
      <div class="sidebar-header">
        <div class="sidebar-logo">
          <div class="sidebar-logo__icon">W</div>
          <span>管理后台</span>
        </div>
      </div>
      <div class="sidebar-menu">
        <WMenu :items="menuItems" :active-key="activeMenu" @select="handleMenuSelect" />
      </div>
    </div>

    <div class="admin-main">
      <header class="admin-topbar">
        <div class="topbar-left">
          <WBreadcrumb :items="breadcrumbItems" @navigate="handleBreadcrumbNav" />
        </div>
        <div class="topbar-right">
          <button class="topbar-btn" @click="goToChat">
            <WIcon name="chat" size="14" /> 返回对话
          </button>
          <div class="topbar-user">
            <WAvatar :label="nickname || username" size="small" />
            <span>{{ nickname || username }}</span>
          </div>
          <button class="topbar-btn topbar-btn--logout" @click="handleLogout">
            <WIcon name="logout" size="14" /> 退出
          </button>
        </div>
      </header>
      <main class="admin-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuth } from '../../store/auth'

const router = useRouter()
const route = useRoute()
const { user, logout } = useAuth()
const { nickname, username } = user

const menuItems = [
  { key: '/admin/dashboard', label: '仪表盘', icon: 'dashboard' },
  { key: '/admin/documents', label: '文档管理', icon: 'document' },
  { key: '/admin/audit', label: '审计日志', icon: 'log' },
  { key: '/admin/config', label: '系统配置', icon: 'settings' },
  { key: '/admin/users', label: '用户管理', icon: 'users' }
]

const pageTitles = {
  '/admin/dashboard': '仪表盘',
  '/admin/documents': '文档管理',
  '/admin/audit': '审计日志',
  '/admin/config': '系统配置',
  '/admin/users': '用户管理'
}

const activeMenu = computed(() => route.path)

const breadcrumbItems = computed(() => [
  { label: '首页', to: '/admin/dashboard' },
  { label: pageTitles[route.path] || '' }
])

function handleMenuSelect(key) {
  router.push(key)
}

function handleBreadcrumbNav(to) {
  router.push(to)
}

function goToChat() {
  router.push('/chat')
}

async function handleLogout() {
  logout()
  await router.push('/login')
}
</script>

<style scoped>
.admin-layout { display: flex; min-height: 100vh; }

.admin-sidebar {
  width: 220px;
  background: var(--color-sidebar-bg);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}
.sidebar-header {
  padding: 20px 16px;
  border-bottom: 1px solid rgba(255,255,255,0.06);
}
.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 10px;
}
.sidebar-logo__icon {
  width: 28px;
  height: 28px;
  background: var(--gradient-primary);
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 700;
  font-size: 14px;
}
.sidebar-logo span {
  color: #fff;
  font-weight: 600;
  font-size: 14px;
}
.sidebar-menu { flex: 1; padding: 12px 8px; }

.admin-main { flex: 1; display: flex; flex-direction: column; min-width: 0; }

.admin-topbar {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}

.topbar-right { display: flex; align-items: center; gap: 16px; }

.topbar-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--duration-fast);
  font-family: var(--font-body);
  background: var(--color-surface);
}
.topbar-btn:hover { border-color: var(--color-primary-300); color: var(--color-primary-600); }
.topbar-btn--logout:hover { border-color: var(--color-danger); color: var(--color-danger); }

.topbar-user {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px 4px 4px;
  background: var(--color-bg);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.admin-content {
  flex: 1;
  overflow-y: auto;
  padding: 28px;
  background: var(--color-bg);
}
</style>
